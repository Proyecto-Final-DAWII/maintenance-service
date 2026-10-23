package pe.edu.cibertec.maintenance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "asset-service", url = "http://localhost:8081")
public interface AssetClient {

    @GetMapping("/api/assets/{id}")
    Object getAssetById(@PathVariable("id") Long id);
}