package pe.edu.cibertec.maintenance.repository;

import pe.edu.cibertec.maintenance.entity.Maintenance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MaintenanceRepositoryTest {

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Test
    public void testInsertAndFind() {
        Maintenance m = new Maintenance();
        m.setAssetId(1L);
        m.setMaintenanceType("Preventivo");
        m.setDescription("Revision general de equipos");
        m.setPriority("Alta");
        m.setStartDate(LocalDate.now());
        m.setStatus("EN_PROCESO");

        Maintenance saved = maintenanceRepository.save(m);
        assertNotNull(saved.getId());

        Optional<Maintenance> found = maintenanceRepository.findById(saved.getId());
        assertTrue(found.isPresent());
    }

    @Test
    public void testList() {
        List<Maintenance> list = maintenanceRepository.findAll();
        assertNotNull(list);
    }

    @Test
    public void testUpdate() {
        Maintenance m = new Maintenance();
        m.setAssetId(2L);
        m.setMaintenanceType("Correctivo");
        m.setDescription("Reparacion de pantalla");
        m.setPriority("Media");
        m.setStartDate(LocalDate.now());
        m.setStatus("PENDIENTE");
        Maintenance saved = maintenanceRepository.save(m);

        saved.setStatus("COMPLETADO");
        Maintenance updated = maintenanceRepository.save(saved);
        assertEquals("COMPLETADO", updated.getStatus());
    }

    @Test
    public void testDelete() {
        Maintenance m = new Maintenance();
        m.setAssetId(3L);
        m.setMaintenanceType("Preventivo");
        m.setDescription("Limpieza interna");
        m.setPriority("Baja");
        m.setStartDate(LocalDate.now());
        m.setStatus("ACTIVO");
        Maintenance saved = maintenanceRepository.save(m);

        maintenanceRepository.deleteById(saved.getId());
        Optional<Maintenance> found = maintenanceRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}