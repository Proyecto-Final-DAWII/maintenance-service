# Guía de estructura de `assignment-service`

Este documento explica cómo está organizado el proyecto, qué responsabilidad tiene cada paquete y dónde debe colocarse cada clase nueva.

La aplicación usa una arquitectura por capas. La regla más importante es que cada capa tenga una sola responsabilidad:

```text
Petición HTTP
    ↓
controller → service → repository → base de datos
                 ├──→ client → otro microservicio
                 └──→ messaging.producer → RabbitMQ

RabbitMQ → messaging.consumer → service
```

El `controller` recibe la entrada, el `service` ejecuta el caso de negocio y el `repository` accede a los datos. OpenFeign y RabbitMQ son mecanismos de integración y no deben contener reglas de negocio.

---

## Estructura general

```text
src/main/java/pe/edu/cibertec/assignment
├── AssignmentServiceApplication.java
├── client
│   └── dto
├── config
│   ├── openfeign
│   └── rabbitmq
├── controller
├── dto
│   ├── common
│   ├── request
│   └── response
├── entity
├── exception
├── mapper
├── messaging
│   ├── consumer
│   ├── event
│   └── producer
├── repository
├── service
│   └── impl
└── util
```

---

## ¿Qué hace cada paquete?

### Paquete raíz: `pe.edu.cibertec.assignment`

Contiene el punto de inicio de la aplicación:

- `AssignmentServiceApplication.java`: inicia Spring Boot, realiza el escaneo de componentes y habilita los clientes OpenFeign.

No deben colocarse aquí controllers, servicios, entidades ni clases auxiliares. Esas clases pertenecen a los paquetes especializados que se describen a continuación.

### `controller`

Es la puerta de entrada HTTP de la aplicación. Aquí van las clases anotadas con `@RestController`.

Responsabilidades:

- Definir rutas como `GET /assignments/{id}` o `POST /assignments`.
- Recibir parámetros, headers y DTOs de entrada.
- Ejecutar validaciones de formato con `@Valid`.
- Llamar a un servicio.
- Devolver el código HTTP y el DTO de respuesta correspondiente.

Ejemplos de nombres:

```text
AssignmentController.java
TeacherAssignmentController.java
```

Un controller no debe implementar reglas de negocio, acceder directamente a un repository, construir consultas SQL ni llamar directamente a RabbitMQ.

### `dto`

Contiene los objetos usados para transportar información dentro de la API sin exponer directamente las entidades de base de datos.

#### `dto.request`

Aquí van los cuerpos de las peticiones HTTP que llegan al servicio.

Ejemplos:

```text
CreateAssignmentRequest.java
UpdateAssignmentRequest.java
AssignStudentRequest.java
```

Es el lugar correcto para validaciones de entrada como `@NotBlank`, `@NotNull`, `@Size` o `@Positive`.

#### `dto.response`

Aquí van los objetos que la API devuelve al cliente.

Ejemplos:

```text
AssignmentResponse.java
AssignmentDetailResponse.java
StudentAssignmentResponse.java
```

Las respuestas deben exponer solamente la información que forma parte del contrato de la API. No se recomienda retornar una clase de `entity` directamente.

#### `dto.common`

Contiene estructuras reutilizadas por distintos endpoints.

Ejemplos:

```text
ApiResponse.java
PageResponse.java
ErrorResponse.java
```

No debe convertirse en un depósito de DTOs sin relación. Si un DTO representa una entrada o salida concreta, debe ir en `request` o `response`.

### `service`

Contiene los contratos de los casos de negocio. Normalmente son interfaces que expresan qué operaciones ofrece la aplicación.

Ejemplo:

```java
public interface AssignmentService {
    AssignmentResponse findById(Long id);
    AssignmentResponse create(CreateAssignmentRequest request);
}
```

Un servicio puede coordinar repositories, mappers, clientes OpenFeign y productores RabbitMQ. No debe depender de detalles HTTP como `HttpServletRequest`, `ResponseEntity` o anotaciones de rutas.

### `service.impl`

Contiene las implementaciones de las interfaces definidas en `service`.

Aquí van:

- Reglas y validaciones de negocio.
- Coordinación de operaciones de lectura y escritura.
- Límites transaccionales con `@Transactional`.
- Llamadas a repositories, mappers, clientes externos y productores de eventos.

Ejemplo de nombre:

```text
AssignmentServiceImpl.java
```

Si una operación responde preguntas como “¿se puede crear?”, “¿qué estado corresponde?” o “¿qué sucede después de guardar?”, esa decisión pertenece a esta capa.

### `repository`

Es la capa de acceso a base de datos. Aquí van interfaces que extienden `JpaRepository`, `CrudRepository` u otra abstracción de persistencia.

Ejemplo:

```java
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByStudentId(Long studentId);
}
```

Los repositories consultan y guardan entidades. No deben devolver DTOs de controllers, llamar a otros microservicios ni publicar eventos.

### `entity`

Contiene el modelo persistido en la base de datos: clases con anotaciones como `@Entity`, `@Table`, `@Id` y relaciones JPA.

Ejemplos:

```text
Assignment.java
AssignmentStatus.java
```

Una entidad representa cómo se almacena la información. No debe utilizarse como request o response HTTP, porque un cambio en la base de datos no debería modificar accidentalmente el contrato público de la API.

### `mapper`

Contiene las conversiones entre entidades, DTOs y, cuando corresponda, modelos de integraciones.

Ejemplos:

```text
AssignmentMapper.java
StudentAssignmentMapper.java
```

Un mapper puede implementarse manualmente o mediante una librería de mapeo. Debe limitarse a transformar datos; no debe consultar la base de datos ni decidir reglas de negocio.

### `client`

Contiene las interfaces declaradas con `@FeignClient` para comunicarse de forma HTTP con otros microservicios.

Ejemplos:

```text
UserClient.java
CourseClient.java
```

Los clientes de este proyecto deben ubicarse bajo este paquete porque `@EnableFeignClients` lo utiliza como paquete de escaneo. Un cliente define el contrato remoto, pero no contiene lógica de negocio.

### `client.dto`

Contiene los requests y responses pertenecientes al contrato de otros microservicios.

Ejemplos:

```text
UserClientResponse.java
CourseClientResponse.java
ValidateEnrollmentRequest.java
```

Estos DTOs se mantienen separados de `dto.request` y `dto.response` porque representan un contrato externo que puede cambiar independientemente de la API de `assignment-service`.

### `config`

Agrupa configuración técnica de Spring y de las integraciones. Aquí van clases que registran beans, properties e interceptores de infraestructura.

No deben colocarse reglas de negocio ni implementaciones de casos de uso en este paquete.

#### `config.openfeign`

Configura las llamadas HTTP realizadas mediante OpenFeign.

Actualmente contiene:

- `OpenFeignConfiguration`: registra la configuración compartida.
- `AuthTokenRequestInterceptor`: propaga el header `Authorization` de la petición actual.
- `HELP.md`: explica cómo declarar y utilizar clientes Feign.

Guía completa: [`config/openfeign/HELP.md`](src/main/java/pe/edu/cibertec/assignment/config/openfeign/HELP.md).

#### `config.rabbitmq`

Configura exchanges, queues, bindings, serialización y publicación de mensajes RabbitMQ.

Actualmente contiene:

- `RabbitConfiguration`: crea los beans principales de RabbitMQ.
- `RabbitProperties`: representa las rutas declaradas en properties.
- `RabbitRoutesRegistrar`: registra dinámicamente cada ruta como bean.
- `RabbitMqTemplate`: valida y prepara los eventos antes de publicarlos.
- `RabbitMqDto`: clase base para los eventos enviados.
- `HELP.md`: guía detallada de publicación y consumo.

Guía completa: [`config/rabbitmq/HELP.md`](src/main/java/pe/edu/cibertec/assignment/config/rabbitmq/HELP.md).

### `messaging`

Agrupa las clases propias de la comunicación asíncrona con RabbitMQ. La configuración técnica permanece en `config.rabbitmq`; los mensajes del negocio permanecen aquí.

#### `messaging.event`

Contiene los contratos de los eventos enviados o recibidos.

Ejemplos:

```text
AssignmentCreatedEvent.java
AssignmentCompletedEvent.java
AssignmentEventPayload.java
```

Los eventos publicados deben respetar la estructura definida por `RabbitMqDto<T>`.

#### `messaging.producer`

Contiene componentes responsables de construir o recibir un evento ya construido y publicarlo en RabbitMQ.

Ejemplos:

```text
AssignmentEventProducer.java
NotificationProducer.java
```

El producer conoce la ruta de RabbitMQ y realiza el envío. La decisión de cuándo publicar pertenece al service.

#### `messaging.consumer`

Contiene métodos anotados con `@RabbitListener` que reciben eventos desde una cola.

Ejemplos:

```text
AssignmentCreatedConsumer.java
StudentUpdatedConsumer.java
```

Un consumer debe validar o adaptar el mensaje y delegar el trabajo al service. No debería contener un caso de negocio completo dentro del listener.

### `exception`

Contiene errores propios de la aplicación y su traducción a respuestas HTTP.

Aquí pueden ir:

- Excepciones como `AssignmentNotFoundException` o `InvalidAssignmentStateException`.
- Un `GlobalExceptionHandler` anotado con `@RestControllerAdvice`.
- Clases auxiliares necesarias para construir respuestas de error.

No uses excepciones genéricas como reemplazo de una regla de negocio clara. Los mensajes sensibles o los tokens de autenticación nunca deben incluirse en la respuesta.

### `util`

Contiene funciones pequeñas, reutilizables y sin estado que no pertenecen a una capa concreta.

Ejemplos adecuados:

```text
DateTimeUtils.java
StringNormalizer.java
```

Este paquete no debe usarse para esconder servicios, acceso a datos o reglas de negocio. Si una clase necesita repositories u otros beans para funcionar, probablemente pertenece a `service` y no a `util`.

---

## Dónde colocar una funcionalidad nueva

Para implementar, por ejemplo, la creación de una asignación:

1. Crea `CreateAssignmentRequest` en `dto.request`.
2. Crea `AssignmentResponse` en `dto.response`.
3. Crea o actualiza `Assignment` en `entity`.
4. Define `AssignmentRepository` en `repository`.
5. Define el contrato `AssignmentService` en `service`.
6. Implementa las reglas en `service.impl.AssignmentServiceImpl`.
7. Convierte entidad y DTO mediante `mapper.AssignmentMapper`.
8. Expón el endpoint desde `controller.AssignmentController`.
9. Si se debe avisar a otro sistema, crea el evento en `messaging.event` y publícalo mediante `messaging.producer` desde el service.
10. Si se debe consultar otro microservicio, crea su interfaz en `client` y sus contratos en `client.dto`.

---

## Reglas rápidas de dependencia

| Desde | Puede usar normalmente | Debe evitar |
|---|---|---|
| `controller` | `service`, `dto` | `repository`, `entity`, RabbitMQ directo |
| `service.impl` | `repository`, `mapper`, `client`, `messaging.producer`, `dto` | Detalles de HTTP y controllers |
| `repository` | `entity` | Controllers, clientes y producers |
| `mapper` | `entity`, `dto`, `client.dto` | Consultas y reglas de negocio |
| `client` | `client.dto` | Entidades y lógica de negocio |
| `messaging.consumer` | `messaging.event`, `service` | Implementar todo el negocio en el listener |
| `messaging.producer` | `messaging.event`, configuración RabbitMQ | Decidir cuándo ocurre el evento |

Reglas generales:

- No expongas entidades JPA directamente desde un controller.
- No accedas a un repository directamente desde un controller.
- No coloques reglas de negocio en DTOs, mappers, clientes o configuración.
- Usa inyección por constructor para las dependencias.
- Mantén separados el contrato HTTP propio, el contrato de otros servicios y los eventos RabbitMQ.
- Los tests deben reflejar la misma estructura bajo `src/test/java/pe/edu/cibertec/assignment`.

---

## Configuración y recursos

El archivo `src/main/resources/application.properties` contiene la configuración de la aplicación, como nombre del servicio, URLs externas, conexión a base de datos y rutas RabbitMQ.

No deben escribirse contraseñas, tokens ni secretos reales directamente en este archivo. Usa variables de entorno:

```properties
app.clients.user-service.url=${USER_SERVICE_URL:http://localhost:8081}
spring.datasource.password=${DB_PASSWORD}
```

---

## Documentación de referencia

Documentación oficial de las tecnologías principales:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.1/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.1/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring for RabbitMQ](https://docs.spring.io/spring-boot/4.1.1/reference/messaging/amqp.html)
* [OpenFeign](https://docs.spring.io/spring-cloud-openfeign/reference/)

---

# OpenFeign con propagación de autenticación

OpenFeign está habilitado para las interfaces ubicadas en `pe.edu.cibertec.assignment.client`. Cada llamada propaga automáticamente el header `Authorization` de la petición HTTP actual, sin registrar ni almacenar el token.

La guía completa, con ejemplos de clientes, properties, credenciales de servicio y diagnóstico, está en:

[`src/main/java/pe/edu/cibertec/assignment/config/openfeign/HELP.md`](src/main/java/pe/edu/cibertec/assignment/config/openfeign/HELP.md)

---

# Guía Rápida de RabbitMQ

> **Nota:** La guía completa con ejemplos paso a paso se encuentra en:  
> [`src/main/java/pe/edu/cibertec/assignment/config/rabbitmq/HELP.md`](src/main/java/pe/edu/cibertec/assignment/config/rabbitmq/HELP.md).

No necesitas registrar manualmente beans para `@Bean Queue`, `@Bean TopicExchange` ni `@Bean Binding`. Todo se genera automáticamente desde `application.properties`.

---

## DTO Base Obligatorio: `RabbitMqDto<T>`

Todos los eventos deben heredar obligatoriamente de [`RabbitMqDto<T>`](src/main/java/pe/edu/cibertec/assignment/config/rabbitmq/RabbitMqDto.java):

```java
package pe.edu.cibertec.assignment.config.rabbitmq;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class RabbitMqDto<T> implements Serializable {
    @JsonProperty("data")
    private transient T data; // Payload de negocio (editable desde afuera)
    @Setter(AccessLevel.PROTECTED)
    private String traceId; // UUID automático
    @Setter(AccessLevel.PROTECTED)
    private String operation; // Nombre de la routing key / topic automático
    @Setter(AccessLevel.PROTECTED)
    private String type; // SUCCESS / ERROR / EVENT automático
    private String message; // Mensaje opcional
    private String errorCode; // Código de error opcional (ej: "ERR-4001")
    @Setter(AccessLevel.PROTECTED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp; // Fecha y hora automática
}
```

---

## Productor con Validación y Headers Personalizados

`rabbitTemplate.convertAndSend` valida automáticamente que el objeto herede de `RabbitMqDto<?>` y soporta agregar cabeceras personalizadas con `Map<String, Object>`:

```java
@Service
public class OrderProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties.Route ordersRoute;

    public OrderProducerService(
            RabbitTemplate rabbitTemplate,
            @Qualifier("ordersRoute") RabbitProperties.Route ordersRoute) {
        this.rabbitTemplate = rabbitTemplate;
        this.ordersRoute = ordersRoute;
    }

    public void sendOrder(OrderPayload payload) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .data(payload)
                .message("Nueva orden creada")
                .build();

        // Envío con headers personalizados
        Map<String, Object> headers = Map.of(
                "X-Tenant-Id", "PE-01",
                "X-App", "assignment-service"
        );

        rabbitTemplate.convertAndSend(
                ordersRoute.getExchange(),
                ordersRoute.getRoutingKey(),
                event,
                message -> {
                    headers.forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
                    return message;
                }
        );
    }
}
```

---

## Consumidor con Headers

```java
@Component
public class OrderConsumerListener {

    @RabbitListener(queues = "#{@ordersQueue.name}")
    public void receiveOrder(
            OrderCreatedEvent event,
            @Header(name = "X-Tenant-Id", required = false) String tenantId) {
        System.out.println("TraceId: " + event.getTraceId());
        System.out.println("Tenant:  " + tenantId);
        System.out.println("Payload: " + event.getData());
    }
}
```

---

## Cheat-Sheet de `@Qualifier` Disponibles

| Key en properties | Cola (`Queue`) | Intercambio (`TopicExchange`) | Enlace (`Binding`) | Configuración (`Route`) |
|---|---|---|---|---|
| `orders` | `@Qualifier("ordersQueue")` | `@Qualifier("ordersExchange")` | `@Qualifier("ordersBinding")` | `@Qualifier("ordersRoute")` o `@Qualifier("orders")` |
| `payments` | `@Qualifier("paymentsQueue")` | `@Qualifier("paymentsExchange")` | `@Qualifier("paymentsBinding")` | `@Qualifier("paymentsRoute")` o `@Qualifier("payments")` |
| `notifications` | `@Qualifier("notificationsQueue")` | `@Qualifier("notificationsExchange")` | `@Qualifier("notificationsBinding")` | `@Qualifier("notificationsRoute")` o `@Qualifier("notifications")` |
| `notifications-success` | `@Qualifier("notificationsSuccessQueue")` | `@Qualifier("notificationsSuccessExchange")` | `@Qualifier("notificationsSuccessBinding")` | `@Qualifier("notificationsSuccessRoute")` |
| `notifications-error` | `@Qualifier("notificationsErrorQueue")` | `@Qualifier("notificationsErrorExchange")` | `@Qualifier("notificationsErrorBinding")` | `@Qualifier("notificationsErrorRoute")` |
