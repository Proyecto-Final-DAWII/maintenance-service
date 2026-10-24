package pe.edu.cibertec.maintenance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cibertec.maintenance.client.AssetClient;
import pe.edu.cibertec.maintenance.client.dto.AssetResponseDto;
import pe.edu.cibertec.maintenance.client.dto.AssetStatusRequestDto;
import pe.edu.cibertec.maintenance.dto.request.MaintenanceRequestDto;
import pe.edu.cibertec.maintenance.entity.Maintenance;
import pe.edu.cibertec.maintenance.mapper.MaintenanceMapper;
import pe.edu.cibertec.maintenance.messaging.producer.MaintenanceProducer;
import pe.edu.cibertec.maintenance.repository.MaintenanceRepository;
import pe.edu.cibertec.maintenance.service.impl.MaintenanceServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository repository;
    @Mock
    private MaintenanceMapper mapper;
    @Mock
    private AssetClient assetClient;
    @Mock
    private MaintenanceProducer producer;

    @InjectMocks
    private MaintenanceServiceImpl service;

    @Test
    void startsMaintenanceAndUpdatesInventory() {
        MaintenanceRequestDto request = new MaintenanceRequestDto();
        request.setAssetId("LAP-001");
        AssetResponseDto asset = new AssetResponseDto();
        asset.setId("LAP-001");
        asset.setStatus("DISPONIBLE");
        Maintenance entity = new Maintenance();
        entity.setAssetId("LAP-001");
        when(assetClient.getAssetById("LAP-001")).thenReturn(asset);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponseDto(entity)).thenCallRealMethod();

        var response = service.createMaintenance(request);

        assertThat(response.getStatus()).isEqualTo("EN_PROCESO");
        verify(assetClient).updateStatus("LAP-001", new AssetStatusRequestDto("EN_MANTENIMIENTO"));
    }
}
