package pe.edu.cibertec.maintenance.messaging.producer;

import org.springframework.stereotype.Component;
import pe.edu.cibertec.maintenance.config.rabbitmq.RabbitMqTemplate;
import pe.edu.cibertec.maintenance.config.rabbitmq.RabbitProperties;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;
import pe.edu.cibertec.maintenance.messaging.event.MaintenanceEventDto;

import java.time.LocalDateTime;

@Component
public class MaintenanceProducer {

    private final RabbitMqTemplate rabbitMqTemplate;
    private final RabbitProperties rabbitProperties;

    public MaintenanceProducer(RabbitMqTemplate rabbitMqTemplate, RabbitProperties rabbitProperties) {
        this.rabbitMqTemplate = rabbitMqTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    public void publish(MaintenanceResponseDto maintenanceResponse, String previousState, String newState) {
        MaintenanceEventDto event = MaintenanceEventDto.builder()
                .assetId(maintenanceResponse.getAssetId())
                .userId("system")
                .actionType("MANTENIMIENTO_" + maintenanceResponse.getStatus())
                .previousState(previousState)
                .newState(newState)
                .details(maintenanceResponse.getDescription())
                .eventDate(LocalDateTime.now())
                .build();

        RabbitProperties.Route route = rabbitProperties.getRoute("audit");
        rabbitMqTemplate.convertAndSend(route, event);
    }
}
