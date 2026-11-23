export type MesaResumen = {
  id: number;
  numero: number;
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
