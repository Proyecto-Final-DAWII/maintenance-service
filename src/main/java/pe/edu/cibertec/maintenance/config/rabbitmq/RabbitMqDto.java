package pe.edu.cibertec.maintenance.config.rabbitmq;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Clase base obligatoria para todos los mensajes y eventos enviados a través de RabbitMQ.
 *
 * @param <T> Tipo de datos del payload (objeto de negocio contenido)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class RabbitMqDto<T> implements Serializable {

    private static final List<String> TRACE_HEADERS = List.of(
            "X-Trace-Id", "X-Trace-ID", "x-trace-id",
            "X-Correlation-Id", "X-Correlation-ID", "x-correlation-id",
            "X-Request-Id", "X-Request-ID", "x-request-id",
            "traceparent"
    );

    /**
     * Payload de negocio. Se puede editar y acceder desde afuera.
     */
    @JsonProperty("data")
    private transient T data;

    /**
     * Identificador único de trazabilidad (UUID).
     * Se obtiene automáticamente del header de la petición HTTP actual (ej: X-Trace-Id, X-Correlation-Id).
     * Si no existe en la petición o no estamos en un contexto HTTP, se genera un UUID automáticamente.
     * No se puede editar desde afuera.
     */
    @Setter(AccessLevel.PROTECTED)
    private String traceId;

    /**
     * Nombre de la operación derivada de la routing key o exchange.
     * No se puede editar desde afuera.
     */
    @Setter(AccessLevel.PROTECTED)
    private String operation;

    /**
     * Tipo de mensaje (ej: SUCCESS, ERROR, EVENT). Derivado de la routing key.
     * No se puede editar desde afuera.
     */
    @Setter(AccessLevel.PROTECTED)
    private String type;

    /**
     * Mensaje descriptivo opcional. Se puede definir desde afuera.
     */
    private String message;

    /**
     * Código de error opcional (ej: "ERR-4001"). Se puede agregar o dejar null si no hay error.
     */
    private String errorCode;

    /**
     * Fecha y hora de creación del evento. Generado automáticamente con zona horaria del sistema.
     * No se puede editar desde afuera.
     */
    @Setter(AccessLevel.PROTECTED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Prepara y autocompleta los metadatos del evento antes de ser enviado a RabbitMQ.
     */
    public void prepare(String exchange, String routingKey) {
        initTraceId();
        initTimestamp();
        initOperation(exchange, routingKey);
        initType(routingKey);
    }

    private void initTraceId() {
        if (this.traceId == null || this.traceId.isBlank()) {
            this.traceId = resolveCurrentTraceId();
        }
    }

    private void initTimestamp() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    private void initOperation(String exchange, String routingKey) {
        if (this.operation != null && !this.operation.isBlank()) {
            return;
        }
        this.operation = (routingKey != null && !routingKey.isBlank()) ? routingKey : exchange;
    }

    private void initType(String routingKey) {
        if (this.type != null && !this.type.isBlank()) {
            return;
        }
        this.type = determineType(routingKey);
    }

    private static String determineType(String routingKey) {
        if (routingKey == null) {
            return "EVENT";
        }
        String lower = routingKey.toLowerCase();
        if (lower.contains("error")) {
            return "ERROR";
        }
        if (lower.contains("success")) {
            return "SUCCESS";
        }
        return "EVENT";
    }

    /**
     * Resuelve el traceId intentando extraerlo de:
     * 1. Cabeceras de la petición HTTP actual (X-Trace-Id, X-Correlation-Id, X-Request-Id, traceparent).
     * 2. MDC (Logging context).
     * 3. Fallback: genera un nuevo UUID.
     */
    public static String resolveCurrentTraceId() {
        String httpTraceId = extractTraceIdFromHttp();
        if (httpTraceId != null) {
            return httpTraceId;
        }

        String mdcTraceId = extractTraceIdFromMdc();
        if (mdcTraceId != null) {
            return mdcTraceId;
        }

        return UUID.randomUUID().toString();
    }

    private static String extractTraceIdFromHttp() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletAttributes) {
                return findTraceHeader(servletAttributes.getRequest());
            }
        } catch (Exception ignored) {
            // Ignorado en ejecuciones sin contexto HTTP
        }
        return null;
    }

    private static String findTraceHeader(HttpServletRequest request) {
        for (String headerName : TRACE_HEADERS) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                return parseHeaderValue(headerName, value.trim());
            }
        }
        return null;
    }

    private static String parseHeaderValue(String headerName, String value) {
        if (!"traceparent".equalsIgnoreCase(headerName)) {
            return value;
        }
        return parseTraceparent(value);
    }

    private static String parseTraceparent(String value) {
        String[] parts = value.split("-");
        if (parts.length >= 2 && !parts[1].isBlank()) {
            return parts[1].trim();
        }
        return value;
    }

    private static String extractTraceIdFromMdc() {
        try {
            String mdcTraceId = MDC.get("traceId");
            if (mdcTraceId != null && !mdcTraceId.isBlank()) {
                return mdcTraceId.trim();
            }
            String mdcTraceIdAlt = MDC.get("trace_id");
            if (mdcTraceIdAlt != null && !mdcTraceIdAlt.isBlank()) {
                return mdcTraceIdAlt.trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
