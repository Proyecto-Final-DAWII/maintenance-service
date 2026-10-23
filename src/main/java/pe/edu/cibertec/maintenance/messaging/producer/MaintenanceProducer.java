package pe.edu.cibertec.maintenance.messaging.producer;

import org.springframework.stereotype.Component;
import pe.edu.cibertec.maintenance.config.rabbitmq.RabbitMqTemplate;
import pe.edu.cibertec.maintenance.config.rabbitmq.RabbitProperties;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;
import pe.edu.cibertec.maintenance.messaging.event.MaintenanceEventDto;

@Component
public class MaintenanceProducer {

    private final RabbitMqTemplate rabbitMqTemplate;
    private final RabbitProperties rabbitProperties;

    public MaintenanceProducer(RabbitMqTemplate rabbitMqTemplate, RabbitProperties rabbitProperties) {
        this.rabbitMqTemplate = rabbitMqTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    public void sendMaintenanceEvent(MaintenanceResponseDto maintenanceResponse, String routeKeyName) {
        MaintenanceEventDto event = MaintenanceEventDto.builder()
                .data(maintenanceResponse)
                .message("Evento de mantenimiento procesado exitosamente")
                .build();

        RabbitProperties.Route route = rabbitProperties.getRoute(routeKeyName);
        rabbitMqTemplate.convertAndSend(route, event);
    }
}