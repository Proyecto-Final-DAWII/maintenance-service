package pe.edu.cibertec.maintenance.mapper;

import org.springframework.stereotype.Component;
import pe.edu.cibertec.maintenance.dto.request.MaintenanceRequestDto;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;
import pe.edu.cibertec.maintenance.entity.Maintenance;

@Component
public class MaintenanceMapper {

    public Maintenance toEntity(MaintenanceRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        Maintenance maintenance = new Maintenance();
        maintenance.setAssetId(requestDto.getAssetId());
        maintenance.setMaintenanceType(requestDto.getMaintenanceType());
        maintenance.setDescription(requestDto.getDescription());
        maintenance.setPriority(requestDto.getPriority());
        maintenance.setStartDate(requestDto.getStartDate());
        maintenance.setStatus(requestDto.getStatus());

        return maintenance;
    }

    public MaintenanceResponseDto toResponseDto(Maintenance entity) {
        if (entity == null) {
            return null;
        }

        MaintenanceResponseDto responseDto = new MaintenanceResponseDto();
        responseDto.setId(entity.getId());
        responseDto.setAssetId(entity.getAssetId());
        responseDto.setMaintenanceType(entity.getMaintenanceType());
        responseDto.setDescription(entity.getDescription());
        responseDto.setPriority(entity.getPriority());
        responseDto.setStartDate(entity.getStartDate());
        responseDto.setStatus(entity.getStatus());
        responseDto.setMessage("Operación exitosa");

        return responseDto;
    }
}