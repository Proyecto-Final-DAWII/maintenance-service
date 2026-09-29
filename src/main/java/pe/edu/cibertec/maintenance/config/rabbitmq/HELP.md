# Guía de RabbitMQ

## 1. ¿Cómo funciona esto en palabras sencillas?

Imagina el sistema de mensajería como un servicio postal:
1. **Exchange (Oficina de Correo):** Recibe tu paquete y mira la etiqueta (**Routing Key**).
2. **Routing Key (Dirección/Etiqueta):** Dice a qué tipo de destinatario corresponde (ejemplo: `orders.created`).
3. **Queue (Buzón):** Donde se guardan los mensajes esperando a que alguien los recoja.
4. **Binding (Regla de ruta):** La conexión entre la Oficina y el Buzón.
5. **Productor:** Quien deja la carta en la oficina.
6. **Consumidor:** Quien revisa el buzón y procesa la carta.

---

## 2. ¿Dónde se configuran las rutas?

Todo vive en [`src/main/resources/application.properties`](file:///Volumes/AskExternalDisk/dev/PF-DAWII/assignment-service/src/main/resources/application.properties):

```properties
# Ruta para órdenes
app.rabbitmq.routes.orders.exchange=orders.exchange
app.rabbitmq.routes.orders.queue=orders.queue
app.rabbitmq.routes.orders.routing-key=orders.created

# Ruta para pagos
app.rabbitmq.routes.payments.exchange=payments.exchange
app.rabbitmq.routes.payments.queue=payments.queue
app.rabbitmq.routes.payments.routing-key=payments.created

# Ruta para notificaciones (Usa comodín '#' para capturar cualquier sub-clave como success o error)
app.rabbitmq.routes.notifications.exchange=notifications.exchange
app.rabbitmq.routes.notifications.queue=notifications.queue
app.rabbitmq.routes.notifications.routing-key=notifications.#
```

> **¿Quieres crear una nueva cola para otro tema (ejemplo: envíos)?**
> Solo agregas 3 líneas en el archivo:
> ```properties
> app.rabbitmq.routes.shipments.exchange=shipments.exchange
> app.rabbitmq.routes.shipments.queue=shipments.queue
> app.rabbitmq.routes.shipments.routing-key=shipments.tracking
> ```
> ¡Y listo! La cola, exchange y binding se crean solos sin tocar código Java.

---

## 3. El DTO Base Obligatorio: `RabbitMqDto<T>`

Todos los eventos y mensajes enviados por RabbitMQ **deben heredar obligatoriamente** de [`RabbitMqDto<T>`](file:///Volumes/AskExternalDisk/dev/PF-DAWII/assignment-service/src/main/java/pe/edu/cibertec/assignment/config/rabbitmq/RabbitMqDto.java):

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
    private transient T data; // se puede editar y obtener desde afuera (tu objeto de negocio)

    /**
     * Identificador único de trazabilidad (UUID).
     * Se extrae automáticamente de las cabeceras de la petición HTTP actual:
     * - X-Trace-Id / x-trace-id
     * - X-Correlation-Id / x-correlation-id
     * - X-Request-Id / x-request-id
     * - traceparent (W3C standard)
     * Si no existe petición HTTP o viene vacío, genera un UUID aleatorio.
     * No se puede editar desde afuera.
     */
    @Setter(AccessLevel.PROTECTED)
    private String traceId;

    @Setter(AccessLevel.PROTECTED)
    private String operation; // se obtiene de la routing key / topic, no se puede editar desde afuera

    @Setter(AccessLevel.PROTECTED)
    private String type; // SUCCESS, ERROR o EVENT (calculado automáticamente), no se puede editar desde afuera

    private String message; // mensaje opcional que puedes agregar desde afuera

    private String errorCode; // código de error opcional (ej: "ERR-404")

    @Setter(AccessLevel.PROTECTED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp; // automático al enviar, no se puede editar desde afuera
}
```

### ¿Cómo crear tu propio DTO de evento heredando de `RabbitMqDto`?

1. Defines tu payload con los datos de negocio:
```java
package pe.edu.cibertec.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayload {
    private String orderId;
    private String customerName;
    private Double totalAmount;
}
```

2. Creas tu clase de evento extendiendo `RabbitMqDto<OrderPayload>`:
```java
package pe.edu.cibertec.assignment.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pe.edu.cibertec.assignment.config.rabbitmq.RabbitMqDto;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class OrderCreatedEvent extends RabbitMqDto<OrderPayload> {
}
```

---

## 4. Cómo ser PRODUCTOR (Enviar mensajes con Validación y Headers)

Gracias a `RabbitMqTemplate`:
1. **Es obligatorio heredar de `RabbitMqDto<?>`**: Si intentas enviar un `String` o un objeto que no herede de `RabbitMqDto`, lanzará una `IllegalArgumentException` inmediatamente.
2. **Propagación del `traceId` de la petición HTTP**: Si la petición HTTP que invoca a tu servicio trae cabeceras como `X-Trace-Id` o `traceparent`, el `RabbitMqDto` tomará exactamente ese mismo `traceId` y lo enviará tanto en el body como en los headers AMQP (`X-Trace-Id`).
3. **Campos automáticos**: Al momento de enviar, `traceId`, `timestamp`, `operation` y `type` se completan solos.
4. **Soporte de Headers personalizados**: Puedes pasar un `Map<String, Object>` con cabeceras HTTP/AMQP (ej: tenant, token, correlationId).
5. **Soporte directo de `Route`**: Puedes pasar el objeto inyectado con `@Qualifier` directamente.

### Ejemplo de Productor:

```java
package pe.edu.cibertec.assignment.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.assignment.config.rabbitmq.RabbitProperties;
import pe.edu.cibertec.assignment.dto.OrderCreatedEvent;
import pe.edu.cibertec.assignment.dto.OrderPayload;

import java.util.Map;

@Service
public class OrderProducerService {

    private static final Logger log = LoggerFactory.getLogger(OrderProducerService.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties.Route ordersRoute;

    public OrderProducerService(
            RabbitTemplate rabbitTemplate,
            @Qualifier("ordersRoute") RabbitProperties.Route ordersRoute) {
        this.rabbitTemplate = rabbitTemplate;
        this.ordersRoute = ordersRoute;
    }

    /**
     * Envío simple usando RabbitMqDto
     */
    public void sendOrder(OrderPayload payload) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .data(payload)
                .message("Orden registrada satisfactoriamente")
                .build();

        // Enviar usando la ruta directamente (traceId proviene del request HTTP automáticamente)
        rabbitTemplate.convertAndSend(ordersRoute.getExchange(), ordersRoute.getRoutingKey(), event);
        log.info("Evento enviado. TraceId generado/propagado: {}", event.getTraceId());
    }

    /**
     * Envío con HEADERS personalizados
     */
    public void sendOrderWithHeaders(OrderPayload payload, String tenantId, String userAuth) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .data(payload)
                .message("Orden corporativa")
                .build();

        // Cabeceras personalizadas
        Map<String, Object> customHeaders = Map.of(
                "X-Tenant-Id", tenantId,
                "X-User-Auth", userAuth,
                "X-Application", "assignment-service"
        );

        // Envío con headers
        rabbitTemplate.convertAndSend(
                ordersRoute.getExchange(),
                ordersRoute.getRoutingKey(),
                event,
                message -> {
                    customHeaders.forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
                    return message;
                }
        );

        log.info("Evento con cabeceras enviado exitosamente.");
    }
}
```

---

## 5. Cómo ser CONSUMIDOR (Recibir mensajes y Headers)

Anota el método con `@RabbitListener(queues = "#{@ordersQueue.name}")`. Puedes recibir tu evento y también las cabeceras si las necesitas:

```java
package pe.edu.cibertec.assignment.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.assignment.dto.OrderCreatedEvent;

import java.util.Map;

@Component
public class OrderConsumerListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumerListener.class);

    @RabbitListener(queues = "#{@ordersQueue.name}")
    public void receiveOrder(
            OrderCreatedEvent event,
            @Header(name = "X-Trace-Id", required = false) String traceIdHeader,
            @Header(name = "X-Tenant-Id", required = false) String tenantId,
            @Headers Map<String, Object> allHeaders) {

        log.info("=========================================");
        log.info(">>> [CONSUMIDOR] Mensaje recibido!");
        log.info("Trace ID (Body)  : {}", event.getTraceId());
        log.info("Trace ID (Header): {}", traceIdHeader);
        log.info("Operation        : {}", event.getOperation());
        log.info("Tipo             : {}", event.getType());
        log.info("Fecha            : {}", event.getTimestamp());
        log.info("Mensaje          : {}", event.getMessage());
        log.info("ID Orden         : {}", event.getData().getOrderId());
        log.info("Cliente          : {}", event.getData().getCustomerName());
        log.info("Monto            : S/. {}", event.getData().getTotalAmount());
        log.info("Header Tenant    : {}", tenantId);
        log.info("=========================================");
    }
}
```

---

## 6. ¿Cómo enviar notificaciones de ÉXITO y de ERROR?

Para éxito y error, el campo `type` se autocalcula según la routing key:
- Si la routing key contiene `success` ➡️ `type = "SUCCESS"`
- Si la routing key contiene `error` ➡️ `type = "ERROR"`

### Enfoque con Colas Dedicadas (Recomendado):

#### 1. En `application.properties`:
```properties
# Éxito
app.rabbitmq.routes.notifications-success.exchange=notifications.exchange
app.rabbitmq.routes.notifications-success.queue=notifications.success.queue
app.rabbitmq.routes.notifications-success.routing-key=notifications.success

# Error
app.rabbitmq.routes.notifications-error.exchange=notifications.exchange
app.rabbitmq.routes.notifications-error.queue=notifications.error.queue
app.rabbitmq.routes.notifications-error.routing-key=notifications.error
```

#### 2. DTO de Notificación:
```java
package pe.edu.cibertec.assignment.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pe.edu.cibertec.assignment.config.rabbitmq.RabbitMqDto;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class NotificationEvent extends RabbitMqDto<Object> {
}
```

#### 3. Productor de Notificaciones:
```java
@Service
public class NotificationProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties.Route successRoute;
    private final RabbitProperties.Route errorRoute;

    public NotificationProducerService(
            RabbitTemplate rabbitTemplate,
            @Qualifier("notificationsSuccessRoute") RabbitProperties.Route successRoute,
            @Qualifier("notificationsErrorRoute") RabbitProperties.Route errorRoute) {
        this.rabbitTemplate = rabbitTemplate;
        this.successRoute = successRoute;
        this.errorRoute = errorRoute;
    }

    public void notifySuccess(Object data, String message) {
        NotificationEvent event = NotificationEvent.builder()
                .data(data)
                .message(message)
                .build();

        // Al enviar a notifications.success, 'type' se calcula automáticamente como "SUCCESS"
        rabbitTemplate.convertAndSend(successRoute.getExchange(), successRoute.getRoutingKey(), event);
    }

    public void notifyError(Object data, String errorMessage, String errorCode) {
        NotificationEvent event = NotificationEvent.builder()
                .data(data)
                .message(errorMessage)
                .errorCode(errorCode)
                .build();

        // Al enviar a notifications.error, 'type' se calcula automáticamente como "ERROR"
        rabbitTemplate.convertAndSend(errorRoute.getExchange(), errorRoute.getRoutingKey(), event);
    }
}
```

#### 4. Consumidores Separados:
```java
@Component
public class NotificationConsumers {

    @RabbitListener(queues = "#{@notificationsSuccessQueue.name}")
    public void onNotificationSuccess(NotificationEvent event) {
        System.out.println("🟢 ÉXITO recibido: " + event.getMessage() + " | TraceId: " + event.getTraceId());
    }

    @RabbitListener(queues = "#{@notificationsErrorQueue.name}")
    public void onNotificationError(NotificationEvent event) {
        System.err.println("🔴 ERROR (" + event.getErrorCode() + "): " + event.getMessage() + " | TraceId: " + event.getTraceId());
    }
}
```

---

## 7. Controlador de Prueba

```java
package pe.edu.cibertec.assignment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.assignment.dto.OrderPayload;
import pe.edu.cibertec.assignment.producer.OrderProducerService;

import java.util.UUID;

@RestController
@RequestMapping("/api/test-rabbitmq")
public class RabbitMqTestController {

    private final OrderProducerService producerService;

    public RabbitMqTestController(OrderProducerService producerService) {
        this.producerService = producerService;
    }

    // Pruébalo enviando la cabecera 'X-Trace-Id: mi-trace-123' desde Postman
    @GetMapping("/publish")
    public ResponseEntity<String> publishTest(
            @RequestParam(defaultValue = "Carlos Perez") String cliente,
            @RequestParam(defaultValue = "150.0") Double monto) {

        OrderPayload payload = new OrderPayload(UUID.randomUUID().toString(), cliente, monto);
        producerService.sendOrderWithHeaders(payload, "tenant-pe-01", "Bearer eyJ...");

        return ResponseEntity.ok("Mensaje enviado con éxito. Revisa la consola para ver el log del consumidor y el Trace ID propagado.");
    }
}
```

---

## 8. Tabla Mágica de `@Qualifier` Disponibles

Para cada entrada en tu `application.properties`:

| Key en properties | Cola (`Queue`) | Intercambio (`TopicExchange`) | Enlace (`Binding`) | Configuración (`Route`) |
|---|---|---|---|---|
| `orders` | `@Qualifier("ordersQueue")` | `@Qualifier("ordersExchange")` | `@Qualifier("ordersBinding")` | `@Qualifier("ordersRoute")` o `@Qualifier("orders")` |
| `payments` | `@Qualifier("paymentsQueue")` | `@Qualifier("paymentsExchange")` | `@Qualifier("paymentsBinding")` | `@Qualifier("paymentsRoute")` o `@Qualifier("payments")` |
| `notifications` | `@Qualifier("notificationsQueue")` | `@Qualifier("notificationsExchange")` | `@Qualifier("notificationsBinding")` | `@Qualifier("notificationsRoute")` o `@Qualifier("notifications")` |
| `notifications-success` | `@Qualifier("notificationsSuccessQueue")` | `@Qualifier("notificationsSuccessExchange")` | `@Qualifier("notificationsSuccessBinding")` | `@Qualifier("notificationsSuccessRoute")` |
| `notifications-error` | `@Qualifier("notificationsErrorQueue")` | `@Qualifier("notificationsErrorExchange")` | `@Qualifier("notificationsErrorBinding")` | `@Qualifier("notificationsErrorRoute")` |
