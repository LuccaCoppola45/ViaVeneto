package ar.viaveneto.acceso;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Toda la API pide una sesión, salvo lo necesario para ingresar.
 * Las páginas del front (index.html, js, css) no pasan por acá: el que no ingresó ve la pantalla del PIN.
 */
@Component
@RequiredArgsConstructor
public class FiltroSesion extends OncePerRequestFilter {

	private static final Set<String> SIN_SESION = Set.of("/api/auth/estado", "/api/auth/ingresar", "/api/auth/primer-usuario");
	private static final String PREFIJO = "Bearer ";

	private final AccesoService accesoService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String ruta = request.getRequestURI();
		return !ruta.startsWith("/api/") || SIN_SESION.contains(ruta);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
			throws ServletException, IOException {
		Optional<UsuarioActual> usuario = token(request).flatMap(accesoService::validar);
		if (usuario.isEmpty()) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"status\":401,\"detail\":\"Tenés que ingresar con tu PIN.\"}");
			return;
		}
		request.setAttribute(UsuarioActual.ATRIBUTO, usuario.get());
		cadena.doFilter(request, response);
	}

	private static Optional<String> token(HttpServletRequest request) {
		String encabezado = request.getHeader("Authorization");
		if (encabezado == null || !encabezado.startsWith(PREFIJO)) {
			return Optional.empty();
		}
		return Optional.of(encabezado.substring(PREFIJO.length()).trim()).filter(t -> !t.isEmpty());
	}
}
