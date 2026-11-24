package ar.viaveneto.salon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.MesaResumen;
import ar.viaveneto.mesa.MesaService;
import ar.viaveneto.mesa.NuevaMesa;
import ar.viaveneto.mesa.NuevoNumero;
import ar.viaveneto.salon.SalonDtos.ElementoForm;
import ar.viaveneto.salon.SalonDtos.PlanoForm;
import ar.viaveneto.salon.SalonDtos.SectorVista;
import ar.viaveneto.salon.SalonDtos.UbicacionMesa;

@SpringBootTest
@Transactional
class SalonTest {

	@Autowired SalonService salonService;
	@Autowired MesaService mesaService;

	@Test
	void elLocalArrancaConSusCuatroSectoresYDiecinueveMesas() {
		assertThat(salonService.salon().sectores()).extracting(SectorVista::nombre)
				.containsExactly("Planta baja", "Arriba", "Cerramiento", "Afuera");
		assertThat(mesaService.listar()).hasSize(19);
	}

	@Test
	void guardarElPlanoMueveMesasYReemplazaElementos() {
		Long arriba = salonService.salon().sectores().get(1).id();
		MesaResumen mesa = mesaService.listar().getFirst();

		salonService.guardarPlano(new PlanoForm(
				List.of(new UbicacionMesa(mesa.id(), arriba, 5000, 100, true)),
				List.of(new ElementoForm(arriba, TipoElemento.BARRA, 10, 10, 200, 60))));

		MesaResumen movida = mesaService.listar().getFirst();
		assertThat(movida.sectorId()).isEqualTo(arriba);
		assertThat(movida.vertical()).isTrue();
		assertThat(movida.x()).as("queda adentro del plano").isEqualTo(Plano.ANCHO - Plano.MESA_CORTO);
		assertThat(salonService.salon().sectores()).flatExtracting(SectorVista::elementos).hasSize(1);
	}

	@Test
	void unaMesaNuevaSeUbicaSinPisarOtra() {
		Long sectorId = salonService.salon().sectores().get(0).id();
		MesaResumen nueva = mesaService.agregar(new NuevaMesa(50, sectorId));

		List<MesaResumen> otras = mesaService.listar().stream()
				.filter(m -> m.sectorId().equals(sectorId) && m.numero() != 50)
				.toList();
		assertThat(otras).noneMatch(o -> nueva.x() < o.x() + Plano.MESA_LARGO && o.x() < nueva.x() + Plano.MESA_LARGO
				&& nueva.y() < o.y() + Plano.MESA_LARGO && o.y() < nueva.y() + Plano.MESA_CORTO);
	}

	@Test
	void noSePuedeRepetirElNumeroDeMesa() {
		MesaResumen mesa = mesaService.listar().getFirst();
		assertThatThrownBy(() -> mesaService.cambiarNumero(mesa.id(), new NuevoNumero(2)))
				.isInstanceOf(ReglaNegocioException.class);
		assertThat(mesaService.cambiarNumero(mesa.id(), new NuevoNumero(40)).numero()).isEqualTo(40);
	}
}
