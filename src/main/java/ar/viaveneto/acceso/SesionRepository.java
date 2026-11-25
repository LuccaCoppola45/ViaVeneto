package ar.viaveneto.acceso;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SesionRepository extends JpaRepository<Sesion, String> {

	@Modifying
	@Query("delete from Sesion s where s.usuario.id = :usuarioId")
	void borrarDe(Long usuarioId);

	@Modifying
	@Query("delete from Sesion s where s.vence < :ahora")
	void borrarVencidas(LocalDateTime ahora);
}
