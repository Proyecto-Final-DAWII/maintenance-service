# Guía de OpenFeign con propagación de autenticación

## 1. ¿Qué resuelve esta configuración?

Cuando un usuario llama a `assignment-service`, normalmente envía su credencial en el header HTTP:

```http
Authorization: Bearer eyJhbGciOi...
```

Si este servicio necesita consultar otro microservicio mediante OpenFeign, la credencial no viaja automáticamente. Esta configuración agrega un interceptor global que copia el header `Authorization` de la petición entrante a la petición saliente:

```text
Cliente
  └─ Authorization: Bearer token
       ↓
assignment-service
  └─ AuthTokenRequestInterceptor
       ↓
otro-microservicio
  └─ Authorization: Bearer token
```

El token se copia completo. Por eso funciona con `Bearer`, `Basic` u otro esquema que ya venga correctamente expresado en `Authorization`.

---

## 2. Archivos involucrados

- `OpenFeignConfiguration.java`: registra el interceptor para todos los clientes Feign.
- `AuthTokenRequestInterceptor.java`: obtiene y propaga el header `Authorization`.
- `AssignmentServiceApplication.java`: habilita el escaneo de clientes con `@EnableFeignClients`.
- `AuthTokenRequestInterceptorTest.java`: valida propagación, ausencia de token y precedencia.

Los clientes deben ubicarse dentro de:

```text
pe.edu.cibertec.assignment.client
```

Ese es el paquete configurado en `@EnableFeignClients`.

---

## 3. Cómo crear un cliente OpenFeign

### 3.1 Configura la URL

Agrega la dirección del servicio remoto en `application.properties`:

```properties
app.clients.user-service.url=http://localhost:8081
```

En un ambiente real puede venir de una variable de entorno:

```properties
app.clients.user-service.url=${USER_SERVICE_URL:http://localhost:8081}
```

### 3.2 Crea el DTO de respuesta

```java
package pe.edu.cibertec.assignment.client.dto;

public record UserResponse(
        Long id,
        String username,
        String email
) {
}
```

### 3.3 Declara el cliente

```java
package pe.edu.cibertec.assignment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.cibertec.assignment.client.dto.UserResponse;

@FeignClient(
        name = "user-service",
        url = "${app.clients.user-service.url}"
)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserResponse findById(@PathVariable Long id);
}
```

No necesitas recibir ni pasar `Authorization` como parámetro del método. El interceptor lo agrega automáticamente.

### 3.4 Usa el cliente desde un servicio

```java
package pe.edu.cibertec.assignment.service;

import org.springframework.stereotype.Service;
import pe.edu.cibertec.assignment.client.UserClient;
import pe.edu.cibertec.assignment.client.dto.UserResponse;

@Service
public class UserQueryService {

    private final UserClient userClient;

    public UserQueryService(UserClient userClient) {
        this.userClient = userClient;
    }

    public UserResponse findUser(Long id) {
        return userClient.findById(id);
    }
}
```

Si `findUser` se ejecuta mientras se atiende una petición HTTP, el token de esa petición se enviará al `user-service`.

---

## 4. Reglas del interceptor

1. Busca el header `Authorization` en la petición HTTP actual.
2. Si contiene texto, lo copia sin cambiar su esquema ni su valor.
3. Si no hay petición HTTP activa, continúa sin agregar el header.
4. Si no existe token o está vacío, continúa sin agregar el header.
5. Si el cliente Feign ya definió su propio `Authorization`, lo conserva y no lo reemplaza.
6. Nunca guarda ni registra el token en logs.

La quinta regla permite que un cliente particular utilice una credencial técnica sin que el token del usuario la sobrescriba.

---

## 5. Cliente con credencial propia

Para una integración que no debe usar el token del usuario, define un interceptor exclusivo:

```java
package pe.edu.cibertec.assignment.client.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

public class PaymentClientConfiguration {

    @Bean
    RequestInterceptor paymentCredentialInterceptor(
            @Value("${app.clients.payment-service.token}") String token) {
        return template -> template.header(
                "Authorization",
                "Bearer " + token
        );
    }
}
```

Y asígnalo solamente a ese cliente:

```java
@FeignClient(
        name = "payment-service",
        url = "${app.clients.payment-service.url}",
        configuration = PaymentClientConfiguration.class
)
public interface PaymentClient {
}
```

La clase del ejemplo se deja deliberadamente sin `@Configuration`: OpenFeign la carga solamente para `PaymentClient`. Si decides anotarla con `@Configuration`, colócala fuera del paquete escaneado por `@SpringBootApplication` o exclúyela del escaneo; de lo contrario su credencial podría aplicarse globalmente.

La propiedad puede tomar el secreto desde una variable de entorno y nunca debe contener un valor real versionado:

```properties
app.clients.payment-service.token=${PAYMENT_SERVICE_TOKEN}
```

---

## 6. Llamadas sin una petición HTTP activa

Un proceso programado con `@Scheduled`, un consumidor RabbitMQ o una tarea asíncrona normalmente no tiene una petición HTTP asociada. En esos casos no existe un token de usuario que propagar y el interceptor no agrega `Authorization`.

Si el servicio remoto exige autenticación, ese cliente debe usar una credencial de servicio como la mostrada en la sección anterior.

El contexto HTTP basado en `RequestContextHolder` tampoco se transfiere automáticamente a otro hilo. Por eso no se debe asumir que `@Async`, `CompletableFuture` o un executor conservarán el token entrante.

---

## 7. Seguridad y diagnóstico

- No escribas el header `Authorization` en logs, mensajes de error o eventos RabbitMQ.
- Usa HTTPS entre servicios fuera de un entorno local.
- Envía el token solamente a servicios confiables.
- Si el servicio remoto responde `401`, verifica que la llamada original incluya `Authorization` y que la invocación Feign ocurra en el mismo hilo HTTP.
- Si responde `403`, el token probablemente llegó, pero no posee el rol o permiso requerido.
- La propagación del token no valida la credencial; la autenticación y autorización siguen siendo responsabilidad de la configuración de seguridad de cada servicio.

---

## 8. Ejecutar las pruebas

```bash
./mvnw test
```

Las pruebas comprueban que:

- el token entrante se copie a Feign;
- no se cree un header vacío;
- funcione fuera de un contexto HTTP;
- una credencial definida explícitamente por un cliente tenga precedencia.
