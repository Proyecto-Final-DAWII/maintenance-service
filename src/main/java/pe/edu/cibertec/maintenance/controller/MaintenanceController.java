package pe.edu.cibertec.maintenance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.maintenance.dto.request.MaintenanceRequestDto;
import pe.edu.cibertec.maintenance.dto.response.MaintenanceResponseDto;
import pe.edu.cibertec.maintenance.service.MaintenanceService;

import java.util.List;

@RestController
@RequestMapping("/api/maintenances")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceResponseDto>> getAllMaintenances() {
        List<MaintenanceResponseDto> maintenances = maintenanceService.findAllMaintenances();
        return ResponseEntity.ok(maintenances);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponseDto> getMaintenanceById(@PathVariable Long id) {
        MaintenanceResponseDto maintenance = maintenanceService.findMaintenanceById(id);
        return ResponseEntity.ok(maintenance);
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponseDto> createMaintenance(@RequestBody MaintenanceRequestDto requestDto) {
        MaintenanceResponseDto created = maintenanceService.createMaintenance(requestDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceResponseDto> updateMaintenance(
            @PathVariable Long id,
            @RequestBody MaintenanceRequestDto requestDto) {
        MaintenanceResponseDto updated = maintenanceService.updateMaintenance(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenance(@PathVariable Long id) {
        maintenanceService.deleteMaintenance(id);
        return ResponseEntity.noContent().build();
    }
}