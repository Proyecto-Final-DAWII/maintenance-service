package pe.edu.cibertec.maintenance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.cibertec.maintenance.entity.Maintenance;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
}