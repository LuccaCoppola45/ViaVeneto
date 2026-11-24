# Via Veneto: alcance acordado

Sistema para reemplazar el papel en el salón, la caja y el control de stock.
Lo usan personas sin conocimientos técnicos: botones grandes, pocos pasos, español rioplatense.

## Cómo trabaja el negocio (relevado el 23/11/2026)

- **Salón:** entre 11 y 20 mesas.
- **Modalidades:** mesas, para llevar y delivery. El delivery puede dejar de existir más adelante.
- **Stock:** mixto. Bebidas y envasados se cuentan por unidad; los platos elaborados descuentan insumos según su receta.
- **Comprobantes:** hoy se hace factura C en papel y la A se manda por mail. La v1 **no** integra ARCA: genera un ticket/precuenta no fiscal con el total.
- **Cobro:** efectivo, tarjeta de débito/crédito y transferencia/QR.
- **Dispositivos:** una PC en la caja (por cable al router) y tablets o celulares para que los mozos tomen pedidos. El wifi del salón es flojo: conviene sumar un repetidor o access point.

## Primera versión (v1)

1. **Mesas:** ver de un vistazo cuáles están libres u ocupadas, abrir y cerrar cada una.
2. **Pedidos:** cargar platos y bebidas por mesa con cantidades. También pedidos para llevar y delivery, sin mesa.
3. **Stock:** se descuenta solo al vender (por unidad o por receta) y avisa cuando algo baja del mínimo.
4. **Tickets:** precuenta/ticket no fiscal imprimible con el total, y registrar con qué se pagó.
5. **Caja automática:** al final del día el sistema muestra cuánto entró en efectivo, en tarjeta y en transferencia, para que la caja no tenga que separarlo y sumarlo a mano.

**Queda para después:** factura electrónica ARCA, comandas impresas en cocina, reportes de ventas.

## Tecnología

- **Backend:** Spring Boot 4 (Java 21, Maven), JPA. MySQL en producción, H2 en desarrollo para no depender de un servidor local.
- **Frontend:** Angular standalone con signals, app web que se abre desde el navegador de la PC, las tablets y los celulares.
- **Deploy:** una sola imagen Docker en Render (igual que la peluquería). Si se corta internet no se pueden cargar pedidos: es el costo de no mantener un servidor en el local.

## Estado

- [x] Parte 1: mesas y pedidos por mesa (con carta editable).
- [x] Plano del local por sector (planta baja 5, arriba 4, cerramiento 6, afuera 4) y modo "Acomodar salón".
- [ ] Parte 2: para llevar y delivery.
- [ ] Parte 3: stock y alertas.
- [ ] Parte 4: ticket, medios de pago y caja del día.
- [ ] Antes de subir a Render: login (está pensado para la red pública).
