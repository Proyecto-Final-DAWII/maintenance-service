package pe.edu.cibertec.maintenance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pe.edu.cibertec.maintenance.client.dto.AssetResponseDto;
import pe.edu.cibertec.maintenance.client.dto.AssetStatusRequestDto;
import pe.edu.cibertec.maintenance.config.openfeign.OpenFeignConfiguration;

@FeignClient(name = "inventory-service", configuration = OpenFeignConfiguration.class)
public interface AssetClient {

    @GetMapping("/api/inventory/{id}")
    AssetResponseDto getAssetById(@PathVariable("id") String id);

    @PutMapping("/api/inventory/{id}/status")
    AssetResponseDto updateStatus(@PathVariable("id") String id, @RequestBody AssetStatusRequestDto request);
}
