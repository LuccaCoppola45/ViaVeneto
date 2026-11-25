# Via Veneto

Sistema de mesas, pedidos, stock y caja para Via Veneto. El alcance acordado está en [docs/ALCANCE.md](docs/ALCANCE.md).

- **Backend:** Spring Boot 4 (Java 21) en la raíz. En desarrollo usa H2 en `./data/`, así que no hace falta MySQL.
- **Frontend:** Angular en `viaveneto-frontend/`.

## Levantarlo en la compu

```bash
./mvnw spring-boot:run
```

```bash
cd viaveneto-frontend && npm start -- --port 4300
```

Abrir http://localhost:4300. El backend corre en el puerto 8081 (el 8080 queda para la peluquería) y Angular le pasa `/api` por el proxy.
La primera vez se carga el local (4 sectores, 19 mesas), una carta y un stock de ejemplo; se editan desde "Carta y mesas" y "Stock".

**Ingreso:** la primera vez que se abre, el sistema pide crear la primera persona con un PIN de 4 números. Las demás se agregan desde "Carta y mesas → Personas". No hay usuarios ni PIN de fábrica.

Para probar desde una tablet en la misma red: `npm start -- --port 4300 --host 0.0.0.0` y entrar a `http://<ip-de-la-compu>:4300`.

## Tests

```bash
./mvnw test
```

## Producción (Render)

El `Dockerfile` arma una sola imagen con el front adentro. Variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (MySQL). Antes de publicarlo hay que sumar el login.
