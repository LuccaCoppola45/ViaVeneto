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
import ar.viaveneto.mesa.PasarMesita;
import ar.viaveneto.pedido.PedidoService;

@SpringBootTest
@Transactional
class MesitasTest {

	@Autowired MesaService mesaService;
	@Autowired PedidoService pedidoService;

	@Test
	void pasarUnaMesitaArmaUnaMesaDeSeis() {
		List<MesaResumen> mesas = mesaService.pasarMesita(mesa("1").id(), new PasarMesita(mesa("2").id()));

		assertThat(buscar(mesas, "2").mesitas()).isEqualTo(3);
		assertThat(buscar(mesas, "1").mesitas()).isEqualTo(1);
	}

	@Test
	void partirYVolverAJuntar() {
		List<MesaResumen> partidas = mesaService.partir(mesa("4").id());
		MesaResumen cuatro = buscar(partidas, "4");
		MesaResumen cuatroB = buscar(partidas, "4B");
		assertThat(cuatro.mesitas()).isEqualTo(1);
		assertThat(cuatroB.mesitas()).isEqualTo(1);
		assertThat(cuatroB.sectorId()).isEqualTo(cuatro.sectorId());

		List<MesaResumen> juntas = mesaService.pasarMesita(cuatroB.id(), new PasarMesita(cuatro.id()));
		assertThat(juntas).extracting(MesaResumen::etiqueta).doesNotContain("4B");
		assertThat(buscar(juntas, "4").mesitas()).isEqualTo(2);

		// Se puede volver a partir: reaparece la 4B
		assertThat(mesaService.partir(cuatro.id())).extracting(MesaResumen::etiqueta).contains("4B");
	}

	@Test
	void unaMesaOcupadaNoSePuedeQuedarSinMesitas() {
		Long cuatro = mesa("4").id();
		mesaService.partir(cuatro);
		MesaResumen cuatroB = mesa("4B");
		pedidoService.abrirMesa(cuatroB.id());

		assertThatThrownBy(() -> mesaService.pasarMesita(cuatroB.id(), new PasarMesita(cuatro)))
				.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void unaMesitaSolaNoSePuedePartir() {
		mesaService.partir(mesa("4").id());
		assertThatThrownBy(() -> mesaService.partir(mesa("4B").id())).isInstanceOf(ReglaNegocioException.class);
	}

	private MesaResumen mesa(String etiqueta) {
		return buscar(mesaService.listar(), etiqueta);
	}

	private static MesaResumen buscar(List<MesaResumen> mesas, String etiqueta) {
		return mesas.stream().filter(m -> m.etiqueta().equals(etiqueta)).findFirst().orElseThrow();
	}
}
