export type MesaResumen = {
  id: number;
  numero: number;
  sectorId: number | null;
  x: number;
  y: number;
  vertical: boolean;
  ocupada: boolean;
  pedidoId: number | null;
  abiertaDesde: string | null;
  cantidadItems: number;
  total: number;
};

export type EstadoPedido = 'ABIERTO' | 'CERRADO' | 'ANULADO';

export type ItemPedido = {
  id: number;
  productoId: number;
  nombre: string;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
};

export type PedidoDetalle = {
  id: number;
  tipo: 'MESA' | 'PARA_LLEVAR' | 'DELIVERY';
  estado: EstadoPedido;
  mesaNumero: number | null;
  abiertoEn: string;
  cerradoEn: string | null;
  items: ItemPedido[];
  total: number;
};

export type Producto = {
  id: number;
  nombre: string;
  precio: number;
  categoriaId: number;
  activo: boolean;
};

export type Categoria = {
  id: number;
  nombre: string;
  orden: number;
  productos: Producto[];
};

export type ProductoForm = Omit<Producto, 'id'>;

export type TipoElemento =
  | 'PUERTA' | 'BARRA' | 'CAJA' | 'COCINA' | 'ESCALERA' | 'BANIO' | 'VENTANA' | 'PARED' | 'PLANTA';

export type Elemento = { tipo: TipoElemento; x: number; y: number; ancho: number; alto: number };

export type Sector = { id: number; nombre: string; orden: number; elementos: Elemento[] };

/** Tamaño del plano y de las mesas, en las unidades del backend. */
export type Medidas = { ancho: number; alto: number; mesaLargo: number; mesaCorto: number };

export type Salon = { medidas: Medidas; sectores: Sector[] };

export type PlanoForm = {
  mesas: { id: number; sectorId: number; x: number; y: number; vertical: boolean }[];
  elementos: (Elemento & { sectorId: number })[];
};
