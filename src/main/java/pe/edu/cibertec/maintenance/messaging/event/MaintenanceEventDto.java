package pe.edu.cibertec.maintenance.messaging.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import pe.edu.cibertec.maintenance.config.rabbitmq.RabbitMqDto;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class MaintenanceEventDto extends RabbitMqDto<MaintenanceResponseDto> {
}