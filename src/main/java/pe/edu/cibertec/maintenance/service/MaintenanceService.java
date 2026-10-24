package pe.edu.cibertec.maintenance.service;

import pe.edu.cibertec.maintenance.dto.request.MaintenanceRequestDto;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;

import java.util.List;

public interface MaintenanceService {
    List<MaintenanceResponseDto> findAllMaintenances();
    MaintenanceResponseDto findMaintenanceById(Long id);
    MaintenanceResponseDto createMaintenance(MaintenanceRequestDto requestDto);
    MaintenanceResponseDto updateMaintenance(Long id, MaintenanceRequestDto requestDto);
    MaintenanceResponseDto completeMaintenance(Long id);
    void deleteMaintenance(Long id);
}
