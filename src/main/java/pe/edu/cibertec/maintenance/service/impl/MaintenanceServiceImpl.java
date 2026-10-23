package pe.edu.cibertec.maintenance.service.impl;

import org.springframework.stereotype.Service;
import pe.edu.cibertec.maintenance.dto.request.MaintenanceRequestDto;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;
import pe.edu.cibertec.maintenance.entity.Maintenance;
import pe.edu.cibertec.maintenance.exception.ResourceNotFoundException;
import pe.edu.cibertec.maintenance.mapper.MaintenanceMapper;
import pe.edu.cibertec.maintenance.repository.MaintenanceRepository;
import pe.edu.cibertec.maintenance.service.MaintenanceService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceMapper maintenanceMapper;

    public MaintenanceServiceImpl(MaintenanceRepository maintenanceRepository, MaintenanceMapper maintenanceMapper) {
        this.maintenanceRepository = maintenanceRepository;
        this.maintenanceMapper = maintenanceMapper;
    }

    @Override
    public List<MaintenanceResponseDto> findAllMaintenances() {
        return maintenanceRepository.findAll().stream()
                .map(maintenanceMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public MaintenanceResponseDto findMaintenanceById(Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento no encontrado con ID: " + id));
        return maintenanceMapper.toResponseDto(maintenance);
    }

    @Override
    public MaintenanceResponseDto createMaintenance(MaintenanceRequestDto requestDto) {
        Maintenance maintenance = maintenanceMapper.toEntity(requestDto);
        Maintenance saved = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponseDto(saved);
    }

    @Override
    public MaintenanceResponseDto updateMaintenance(Long id, MaintenanceRequestDto requestDto) {
        Maintenance existing = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento no encontrado para actualizar con ID: " + id));

        existing.setAssetId(requestDto.getAssetId());
        existing.setMaintenanceType(requestDto.getMaintenanceType());
        existing.setDescription(requestDto.getDescription());
        existing.setPriority(requestDto.getPriority());
        existing.setStartDate(requestDto.getStartDate());
        existing.setStatus(requestDto.getStatus());

        Maintenance updated = maintenanceRepository.save(existing);
        return maintenanceMapper.toResponseDto(updated);
    }

    @Override
    public void deleteMaintenance(Long id) {
        Maintenance existing = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento no encontrado para eliminar con ID: " + id));
        maintenanceRepository.delete(existing);
    }
}