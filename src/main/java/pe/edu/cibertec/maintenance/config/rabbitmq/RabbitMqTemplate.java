package pe.edu.cibertec.maintenance.config.rabbitmq;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

/**
 * Extensión personalizada de RabbitTemplate que:
 * 1. Garantiza que todos los mensajes enviados hereden obligatoriamente de RabbitMqDto<?>.
 * 2. Autocompleta automáticamente traceId (desde headers HTTP si existen o UUID), timestamp, operation y type.
 * 3. Propaga el traceId automáticamente en las cabeceras AMQP ("X-Trace-Id").
 * 4. Permite agregar cabeceras personalizadas mediante Map<String, Object>.
 * 5. Permite publicar directamente usando un objeto RabbitProperties.Route.
 */
public class RabbitMqTemplate extends RabbitTemplate {

    public RabbitMqTemplate(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object object, CorrelationData correlationData)
            throws AmqpException {
        convertAndSend(exchange, routingKey, object, (MessagePostProcessor) null, correlationData);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object message,
                               MessagePostProcessor messagePostProcessor, CorrelationData correlationData)
            throws AmqpException {
        validateAndPrepare(message, exchange, routingKey);
        super.convertAndSend(exchange, routingKey, message, m -> {
            if (message instanceof RabbitMqDto<?> dto && dto.getTraceId() != null) {
                m.getMessageProperties().setHeader("X-Trace-Id", dto.getTraceId());
            }
            if (messagePostProcessor != null) {
                return messagePostProcessor.postProcessMessage(m);
            }
            return m;
        }, correlationData);
    }

    /**
     * Envía un evento heredado de RabbitMqDto con cabeceras personalizadas.
     */
    public void convertAndSend(String exchange, String routingKey, RabbitMqDto<?> message,
                               Map<String, Object> headers) throws AmqpException {
        convertAndSend(exchange, routingKey, message, headers, null);
    }

    /**
     * Envía un evento con cabeceras personalizadas y CorrelationData.
     */
    public void convertAndSend(String exchange, String routingKey, RabbitMqDto<?> message,
                               Map<String, Object> headers, CorrelationData correlationData) throws AmqpException {
        validateAndPrepare(message, exchange, routingKey);
        super.convertAndSend(exchange, routingKey, message, m -> {
            if (message.getTraceId() != null) {
                m.getMessageProperties().setHeader("X-Trace-Id", message.getTraceId());
            }
            if (headers != null && !headers.isEmpty()) {
                headers.forEach((key, value) -> m.getMessageProperties().setHeader(key, value));
            }
            return m;
        }, correlationData);
    }

    /**
     * Envía un evento utilizando directamente un RabbitProperties.Route.
     */
    public void convertAndSend(RabbitProperties.Route route, RabbitMqDto<?> message) throws AmqpException {
        convertAndSend(route.getExchange(), route.getRoutingKey(), message, (Map<String, Object>) null);
    }

    /**
     * Envía un evento utilizando un RabbitProperties.Route con cabeceras personalizadas.
     */
    public void convertAndSend(RabbitProperties.Route route, RabbitMqDto<?> message,
                               Map<String, Object> headers) throws AmqpException {
        convertAndSend(route.getExchange(), route.getRoutingKey(), message, headers, null);
    }

    private void validateAndPrepare(Object message, String exchange, String routingKey) {
        if (!(message instanceof RabbitMqDto<?> rabbitMqDto)) {
            throw new IllegalArgumentException(
                    "El mensaje enviado a RabbitMQ debe heredar obligatoriamente de RabbitMqDto<?>. Objeto recibido: "
                            + (message != null ? message.getClass().getName() : "null")
            );
        }
        rabbitMqDto.prepare(exchange, routingKey);
    }
}
