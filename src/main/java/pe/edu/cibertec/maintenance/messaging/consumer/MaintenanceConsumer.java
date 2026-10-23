package pe.edu.cibertec.maintenance.messaging.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.maintenance.messaging.event.MaintenanceEventDto;

@Component
public class MaintenanceConsumer {

    @RabbitListener(queues = "${app.rabbitmq.routes.maintenance.queue:maintenance-queue}")
    public void consumeMaintenanceEvent(
            MaintenanceEventDto event,
            @Header(name = "X-Trace-Id", required = false) String traceId) {

        System.out.println("Mensaje recibido con TraceId: " + traceId);
        if (event != null && event.getData() != null) {
            System.out.println("Procesando mantenimiento ID: " + event.getData().getId());
        }
    }
}