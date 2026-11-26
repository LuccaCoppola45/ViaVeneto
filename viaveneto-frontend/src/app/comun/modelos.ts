export type MesaResumen = {
  id: number;
  numero: number;
  /** "4" o "4B" si la mesa se partió. */
  etiqueta: string;
  /** Mesitas individuales de 2 lugares que la forman. */
  mesitas: number;
  sectorId: number | null;
  x: number;
  y: number;
  vertical: boolean;
  ocupada: boolean;
  pedidoId: number | null;
  /** La moza que atiende la mesa ocupada. */
  atiendeId: number | null;
  atiendeNombre: string | null;
  abiertaDesde: string | null;
  cantidadItems: number;
  total: number;
};

export type EstadoPedido = 'ABIERTO' | 'CERRADO' | 'ANULADO';

export type ItemPedido = {
  id: number;
  productoId: number;
  nombre: string;
  /** Pedido especial para la cocina: "sin cebolla". */
  nota: string | null;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
};

export type MedioPago = 'EFECTIVO' | 'DEBITO' | 'CREDITO' | 'TRANSFERENCIA' | 'RAPPI';

export type Pago = {
  id: number;
  medio: MedioPago;
  monto: number;
  propina: number;
  recibido: number | null;
  vuelto: number;
  fecha: string;
};

export type TipoPedido = 'MESA' | 'PARA_LLEVAR' | 'DELIVERY';

/** Para llevar y Rappi: en qué anda el pedido. */
export type EstadoEntrega = 'PREPARANDO' | 'LISTO' | 'ENTREGADO';

export type PedidoDetalle = {
  id: number;
  tipo: TipoPedido;
  estado: EstadoPedido;
  /** "Mesa 4", "Para llevar · Juan", "Rappi · 1234". */
  titulo: string;
  mesaEtiqueta: string | null;
  cliente: string | null;
  entrega: EstadoEntrega | null;
  atiendeId: number | null;
  atiendeNombre: string | null;
  abiertoEn: string;
  cerradoEn: string | null;
  items: ItemPedido[];
  subtotal: number;
  descuentoPorcentaje: number;
  descuento: number;
  /** Lo que hay que cobrar, ya con el descuento. */
  total: number;
  pagos: Pago[];
  pagado: number;
  /** Lo que falta cobrar. */
  saldo: number;
  /** Ya se hizo la factura en la registradora fiscal. */
  facturado: boolean;
};

export type CobroForm = { medio: MedioPago; monto: number; propina: number; recibido: number | null };

export type TipoTurno = 'MEDIODIA' | 'NOCHE';

export type PorMedio = { medio: MedioPago; cobros: number; monto: number; propina: number };

export type ResumenCaja = {
  porMedio: PorMedio[];
  ventas: number;
  propinas: number;
  descuentos: number;
  mesasCobradas: number;
  /** Pedidos cobrados a los que todavía no se les hizo la factura en la registradora. */
  sinFacturar: { pedidoId: number; titulo: string; total: number }[];
};

export type Turno = {
  id: number;
  tipo: TipoTurno;
  responsable: string;
  automatico: boolean;
  abiertoEn: string;
  cerradoEn: string | null;
  fondoInicial: number;
  resumen: ResumenCaja;
  /** Cambio inicial + ventas en efectivo. */
  efectivoEsperado: number;
  efectivoContado: number | null;
  /** Positivo sobra, negativo falta. */
  diferencia: number | null;
  observaciones: string | null;
};

export type Caja = {
  turnoAbierto: Turno | null;
  turnosDeHoy: Turno[];
  dia: ResumenCaja;
  sugerencia: { tipo: TipoTurno; fondoInicial: number };
};

export type Producto = {
  id: number;
  nombre: string;
  precio: number;
  categoriaId: number;
  activo: boolean;
  /** Cuántos se pueden hacer con el stock; null si el plato no controla stock. */
  disponible: number | null;
};

export type Categoria = {
  id: number;
  nombre: string;
  orden: number;
  productos: Producto[];
};

export type ProductoForm = Omit<Producto, 'id' | 'disponible'>;

export type TipoElemento =
  | 'PUERTA' | 'BARRA' | 'CAJA' | 'COCINA' | 'ESCALERA' | 'BANIO' | 'VENTANA' | 'PARED' | 'PLANTA';

export type Elemento = { tipo: TipoElemento; x: number; y: number; ancho: number; alto: number };

export type Sector = { id: number; nombre: string; orden: number; elementos: Elemento[] };

/** Tamaño del plano y de las mesas, en las unidades del backend. */
export type Medidas = { ancho: number; alto: number; mesitaLargo: number; mesaCorto: number };

export type Salon = { medidas: Medidas; sectores: Sector[] };

export type PlanoForm = {
  mesas: { id: number; sectorId: number; x: number; y: number; vertical: boolean }[];
  elementos: (Elemento & { sectorId: number })[];
};

export type Persona = { id: number; nombre: string };

export type EstadoIngreso = { hayUsuarios: boolean; usuarios: Persona[] };

export type SesionNueva = { token: string; usuario: Persona };

export type Unidad = 'UNIDAD' | 'KILO' | 'LITRO';

export type EstadoStock = 'OK' | 'BAJO' | 'SIN_STOCK';

export type Insumo = {
  id: number;
  nombre: string;
  unidad: Unidad;
  cantidad: number;
  minimo: number;
  estado: EstadoStock;
  /** Platos que lo descuentan. */
  usadoEn: string[];
};

export type LineaReceta = { insumoId: number; insumo: string; unidad: Unidad; cantidad: number };

export type Receta = { productoId: number; producto: string; categoria: string; activo: boolean; lineas: LineaReceta[] };

export type AlertasStock = { bajos: number; sinStock: number };

export type Stock = { insumos: Insumo[]; recetas: Receta[]; alertas: AlertasStock };

export type InsumoForm = { nombre: string; unidad: Unidad; cantidad: number | null; minimo: number };

export type PedidoSinMesa = {
  id: number;
  tipo: TipoPedido;
  titulo: string;
  cliente: string | null;
  estado: EstadoPedido;
  entrega: EstadoEntrega;
  abiertoEn: string;
  unidades: number;
  total: number;
  saldo: number;
  atiendeNombre: string | null;
};
