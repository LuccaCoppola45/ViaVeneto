package ar.viaveneto.acceso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.acceso.AccesoDtos.Ingreso;
import ar.viaveneto.acceso.AccesoDtos.NuevoUsuario;
import ar.viaveneto.acceso.AccesoDtos.SesionVista;
import ar.viaveneto.comun.ReglaNegocioException;

@SpringBootTest
@Transactional
class AccesoTest {

	@Autowired AccesoService accesoService;

	@Test
	void elPrimerUsuarioQuedaAdentroYDespuesNoSePuedeRepetir() {
		assertThat(accesoService.estado().hayUsuarios()).isFalse();
		SesionVista sesion = accesoService.crearPrimerUsuario(new NuevoUsuario("Lucca", "1234"));

		assertThat(accesoService.validar(sesion.token())).map(UsuarioActual::nombre).contains("Lucca");
		assertThatThrownBy(() -> accesoService.crearPrimerUsuario(new NuevoUsuario("Otro", "0000")))
				.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void ingresarConElPinCorrectoYRechazarElIncorrecto() {
		Long id = accesoService.crearPrimerUsuario(new NuevoUsuario("Mamá", "4321")).usuario().id();

		assertThat(accesoService.ingresar(new Ingreso(id, "4321")).token()).isNotBlank();
		assertThatThrownBy(() -> accesoService.ingresar(new Ingreso(id, "1111"))).isInstanceOf(NoAutorizadoException.class);
	}

	@Test
	void despuesDeCincoErroresSeBloqueaAunqueDespuesPongaElBueno() {
		Long id = accesoService.crearPrimerUsuario(new NuevoUsuario("Abuela", "2468")).usuario().id();
		for (int i = 0; i < Usuario.INTENTOS_ANTES_DE_BLOQUEAR; i++) {
			assertThatThrownBy(() -> accesoService.ingresar(new Ingreso(id, "0000"))).isInstanceOf(NoAutorizadoException.class);
		}
		assertThatThrownBy(() -> accesoService.ingresar(new Ingreso(id, "2468")))
				.isInstanceOf(NoAutorizadoException.class)
				.hasMessageContaining("Esperá");
	}

	@Test
	void unTokenInventadoOCerradoNoSirve() {
		SesionVista sesion = accesoService.crearPrimerUsuario(new NuevoUsuario("Lucca", "1234"));
		assertThat(accesoService.validar("inventado")).isEmpty();

		accesoService.salir(sesion.token());
		assertThat(accesoService.validar(sesion.token())).isEmpty();
	}

	@Test
	void alQuitarAAlguienSusSesionesDejanDeServir() {
		SesionVista lucca = accesoService.crearPrimerUsuario(new NuevoUsuario("Lucca", "1234"));
		Long mozoId = accesoService.crear(new NuevoUsuario("Mozo", "5555")).id();
		SesionVista mozo = accesoService.ingresar(new Ingreso(mozoId, "5555"));

		UsuarioActual quienPide = accesoService.validar(lucca.token()).orElseThrow();
		accesoService.quitar(mozoId, quienPide);

		assertThat(accesoService.validar(mozo.token())).isEmpty();
		assertThatThrownBy(() -> accesoService.quitar(quienPide.id(), quienPide)).isInstanceOf(ReglaNegocioException.class);
	}
}
