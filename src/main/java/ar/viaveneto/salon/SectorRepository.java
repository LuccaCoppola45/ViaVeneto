package ar.viaveneto.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

	List<Sector> findAllByOrderByOrdenAscIdAsc();

	boolean existsByNombreIgnoreCase(String nombre);
}
