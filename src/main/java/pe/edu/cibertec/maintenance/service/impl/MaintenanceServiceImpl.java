package pe.edu.cibertec.maintenance.service.impl;

import org.springframework.stereotype.Service;
import pe.edu.cibertec.maintenance.dto.request.MaintenanceRequestDto;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;
import pe.edu.cibertec.maintenance.entity.Maintenance;
import pe.edu.cibertec.maintenance.exception.ResourceNotFoundException;
import pe.edu.cibertec.maintenance.mapper.MaintenanceMapper;
import pe.edu.cibertec.maintenance.repository.MaintenanceRepository;
import pe.edu.cibertec.maintenance.service.MaintenanceService;
import pe.edu.cibertec.maintenance.client.AssetClient;
import pe.edu.cibertec.maintenance.client.dto.AssetStatusRequestDto;
import pe.edu.cibertec.maintenance.messaging.producer.MaintenanceProducer;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final AssetClient assetClient;
    private final MaintenanceProducer maintenanceProducer;

    public MaintenanceServiceImpl(MaintenanceRepository maintenanceRepository, MaintenanceMapper maintenanceMapper,
                                  AssetClient assetClient, MaintenanceProducer maintenanceProducer) {
        this.maintenanceRepository = maintenanceRepository;
        this.maintenanceMapper = maintenanceMapper;
        this.assetClient = assetClient;
        this.maintenanceProducer = maintenanceProducer;
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
    @Transactional
    public MaintenanceResponseDto createMaintenance(MaintenanceRequestDto requestDto) {
        var asset = assetClient.getAssetById(requestDto.getAssetId());
        if (asset == null) {
            throw new ResourceNotFoundException("Activo no encontrado: " + requestDto.getAssetId());
        }
        Maintenance maintenance = maintenanceMapper.toEntity(requestDto);
        maintenance.setStatus("EN_PROCESO");
        Maintenance saved = maintenanceRepository.save(maintenance);
        try {
            assetClient.updateStatus(saved.getAssetId(), new AssetStatusRequestDto("EN_MANTENIMIENTO"));
        } catch (Exception ignored) {}
        MaintenanceResponseDto response = maintenanceMapper.toResponseDto(saved);
        maintenanceProducer.publish(response, asset.getStatus(), "EN_MANTENIMIENTO");
        return response;
    }

    @Override
    @Transactional
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
        MaintenanceResponseDto response = maintenanceMapper.toResponseDto(updated);
        if ("COMPLETADO".equalsIgnoreCase(updated.getStatus())) {
            try {
                assetClient.updateStatus(updated.getAssetId(), new AssetStatusRequestDto("DISPONIBLE"));
            } catch (Exception ignored) {}
            maintenanceProducer.publish(response, "EN_MANTENIMIENTO", "DISPONIBLE");
        }
        return response;
    }

    @Override
    @Transactional
    public MaintenanceResponseDto completeMaintenance(Long id) {
        Maintenance existing = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento no encontrado con ID: " + id));

        existing.setStatus("COMPLETADO");
        Maintenance updated = maintenanceRepository.save(existing);
        MaintenanceResponseDto response = maintenanceMapper.toResponseDto(updated);
        try {
            assetClient.updateStatus(updated.getAssetId(), new AssetStatusRequestDto("DISPONIBLE"));
        } catch (Exception ignored) {}
        maintenanceProducer.publish(response, "EN_MANTENIMIENTO", "DISPONIBLE");
        return response;
    }

    @Override
    public void deleteMaintenance(Long id) {
        Maintenance existing = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento no encontrado para eliminar con ID: " + id));
        maintenanceRepository.delete(existing);
    }
}
