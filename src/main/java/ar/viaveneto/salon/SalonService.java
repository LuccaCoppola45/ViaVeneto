package ar.viaveneto.salon;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.Mesa;
import ar.viaveneto.mesa.MesaRepository;
import ar.viaveneto.salon.SalonDtos.ElementoVista;
import ar.viaveneto.salon.SalonDtos.Medidas;
import ar.viaveneto.salon.SalonDtos.PlanoForm;
import ar.viaveneto.salon.SalonDtos.SalonVista;
import ar.viaveneto.salon.SalonDtos.SectorForm;
import ar.viaveneto.salon.SalonDtos.SectorVista;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalonService {

	private static final Medidas MEDIDAS = new Medidas(Plano.ANCHO, Plano.ALTO, Plano.MESITA_LARGO, Plano.MESA_CORTO);

	private final SectorRepository sectores;
	private final ElementoPlanoRepository elementos;
	private final MesaRepository mesas;

	@Transactional(readOnly = true)
	public SalonVista salon() {
		Map<Long, List<ElementoVista>> porSector = elementos.findAllByOrderById().stream()
				.collect(Collectors.groupingBy(e -> e.getSector().getId(),
						Collectors.mapping(ElementoVista::de, Collectors.toList())));
		List<SectorVista> vistas = sectores.findAllByOrderByOrdenAscIdAsc().stream()
				.map(s -> new SectorVista(s.getId(), s.getNombre(), s.getOrden(), porSector.getOrDefault(s.getId(), List.of())))
				.toList();
		return new SalonVista(MEDIDAS, vistas);
	}

	@Transactional
	public SectorVista crearSector(SectorForm form) {
		String nombre = form.nombre().trim();
		if (sectores.existsByNombreIgnoreCase(nombre)) {
			throw new ReglaNegocioException("Ya existe el sector " + nombre + ".");
		}
		Sector sector = sectores.save(new Sector(nombre, (int) sectores.count() + 1));
		return new SectorVista(sector.getId(), sector.getNombre(), sector.getOrden(), List.of());
	}

	/** Guarda el plano entero: ubica las mesas recibidas y reemplaza los elementos dibujados. */
	@Transactional
	public SalonVista guardarPlano(PlanoForm form) {
		Map<Long, Sector> sectoresPorId = sectores.findAll().stream()
				.collect(Collectors.toMap(Sector::getId, Function.identity()));
		Map<Long, Mesa> mesasPorId = mesas.findAllById(form.mesas().stream().map(m -> m.id()).toList()).stream()
				.collect(Collectors.toMap(Mesa::getId, Function.identity()));

		form.mesas().forEach(u -> {
			Mesa mesa = mesasPorId.get(u.id());
			if (mesa == null) {
				throw new NoEncontradoException("Una de las mesas ya no existe. Recargá la pantalla.");
			}
			mesa.ubicar(sector(sectoresPorId, u.sectorId()), u.x(), u.y(), u.vertical());
		});

		elementos.deleteAllInBatch();
		elementos.saveAll(form.elementos().stream()
				.map(e -> new ElementoPlano(sector(sectoresPorId, e.sectorId()), e.tipo(), e.x(), e.y(), e.ancho(), e.alto()))
				.toList());
		return salon();
	}

	public Sector primerSector() {
		return sectores.findAllByOrderByOrdenAscIdAsc().stream().findFirst()
				.orElseThrow(() -> new ReglaNegocioException("Primero creá un sector del local."));
	}

	public Sector buscarSector(Long id) {
		return sectores.findById(id).orElseThrow(() -> new NoEncontradoException("No existe ese sector."));
	}

	private static Sector sector(Map<Long, Sector> sectoresPorId, Long id) {
		Sector sector = sectoresPorId.get(id);
		if (sector == null) {
			throw new NoEncontradoException("No existe ese sector. Recargá la pantalla.");
		}
		return sector;
	}
}
