package ar.viaveneto.acceso;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.acceso.AccesoDtos.Estado;
import ar.viaveneto.acceso.AccesoDtos.Ingreso;
import ar.viaveneto.acceso.AccesoDtos.NuevoUsuario;
import ar.viaveneto.acceso.AccesoDtos.SesionVista;
import ar.viaveneto.acceso.AccesoDtos.UsuarioVista;
import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.ReglaNegocioException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccesoService {

	/** Una tablet que no se usa en una semana tiene que volver a ingresar. */
	private static final Duration DURACION_SESION = Duration.ofDays(7);
	/** Para no escribir en la base en cada llamada: la sesión se renueva como mucho una vez por hora. */
	private static final Duration RENOVAR_CADA = Duration.ofHours(1);
	private static final int SEGUNDOS_DE_BLOQUEO = 60;
	private static final int BYTES_TOKEN = 32;

	private final UsuarioRepository usuarios;
	private final SesionRepository sesiones;
	private final Clock reloj;
	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
	private final SecureRandom azar = new SecureRandom();

	@Transactional(readOnly = true)
	public Estado estado() {
		List<UsuarioVista> activos = listar();
		return new Estado(!activos.isEmpty(), activos);
	}

	@Transactional(readOnly = true)
	public List<UsuarioVista> listar() {
		return usuarios.findByActivoTrueOrderByNombre().stream().map(UsuarioVista::de).toList();
	}

	/** Solo funciona con el sistema recién instalado: crea a la primera persona y la deja adentro. */
	@Transactional
	public synchronized SesionVista crearPrimerUsuario(NuevoUsuario nuevo) {
		if (usuarios.existsByActivoTrue()) {
			throw new ReglaNegocioException("Ya hay personas cargadas. Ingresá con tu PIN.");
		}
		Usuario usuario = guardarNuevo(nuevo);
		return abrirSesion(usuario);
	}

	@Transactional(noRollbackFor = NoAutorizadoException.class)
	public SesionVista ingresar(Ingreso ingreso) {
		Usuario usuario = usuarios.findById(ingreso.usuarioId())
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoAutorizadoException("No encontramos a esa persona. Recargá la pantalla."));
		if (usuario.bloqueado(ahora())) {
			throw new NoAutorizadoException("Pusiste mal el PIN muchas veces. Esperá un minuto y probá de nuevo.");
		}
		if (!bcrypt.matches(ingreso.pin(), usuario.getPinHash())) {
			usuario.registrarFallo(ahora(), SEGUNDOS_DE_BLOQUEO);
			throw new NoAutorizadoException("PIN incorrecto.");
		}
		usuario.registrarIngreso();
		return abrirSesion(usuario);
	}

	@Transactional
	public void salir(String token) {
		sesiones.deleteById(hash(token));
	}

	/** Valida el token de una llamada. Vacío si no hay sesión o ya venció. */
	@Transactional
	public Optional<UsuarioActual> validar(String token) {
		LocalDateTime ahora = ahora();
		return sesiones.findById(hash(token))
				.filter(s -> s.vigente(ahora))
				.map(s -> {
					LocalDateTime nuevoVencimiento = ahora.plus(DURACION_SESION);
					if (Duration.between(s.getVence(), nuevoVencimiento).compareTo(RENOVAR_CADA) > 0) {
						s.extenderHasta(nuevoVencimiento);
					}
					return new UsuarioActual(s.getUsuario().getId(), s.getUsuario().getNombre());
				});
	}

	@Transactional
	public UsuarioVista crear(NuevoUsuario nuevo) {
		return UsuarioVista.de(guardarNuevo(nuevo));
	}

	@Transactional
	public void cambiarPin(Long usuarioId, String pin) {
		buscar(usuarioId).cambiarPin(bcrypt.encode(pin));
	}

	/** Se saca a alguien que ya no trabaja: sus sesiones abiertas dejan de servir. */
	@Transactional
	public void quitar(Long usuarioId, UsuarioActual quienPide) {
		if (usuarioId.equals(quienPide.id())) {
			throw new ReglaNegocioException("No te podés quitar a vos mismo.");
		}
		if (usuarios.countByActivoTrue() <= 1) {
			throw new ReglaNegocioException("Tiene que quedar al menos una persona.");
		}
		buscar(usuarioId).desactivar();
		sesiones.borrarDe(usuarioId);
	}

	private Usuario guardarNuevo(NuevoUsuario nuevo) {
		String nombre = nuevo.nombre().trim();
		return usuarios.findByNombreIgnoreCase(nombre)
				.map(existente -> {
					if (existente.isActivo()) {
						throw new ReglaNegocioException("Ya hay alguien llamado " + nombre + ".");
					}
					existente.reactivar(bcrypt.encode(nuevo.pin()));
					return existente;
				})
				.orElseGet(() -> usuarios.save(new Usuario(nombre, bcrypt.encode(nuevo.pin()))));
	}

	private SesionVista abrirSesion(Usuario usuario) {
		sesiones.borrarVencidas(ahora());
		byte[] bytes = new byte[BYTES_TOKEN];
		azar.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		sesiones.save(new Sesion(hash(token), usuario, ahora(), ahora().plus(DURACION_SESION)));
		return new SesionVista(token, UsuarioVista.de(usuario));
	}

	private Usuario buscar(Long usuarioId) {
		return usuarios.findById(usuarioId)
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoEncontradoException("No existe esa persona."));
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("La JVM no tiene SHA-256", e);
		}
	}

	private LocalDateTime ahora() {
		return LocalDateTime.now(reloj);
	}
}
