import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import {
  Caja,
  Categoria,
  AlertasStock,
  EstadoEntrega,
  EstadoIngreso,
  PedidoSinMesa,
  TipoPedido,
  Insumo,
  InsumoForm,
  Stock,
  Persona,
  SesionNueva,
  CobroForm,
  MesaResumen,
  PedidoDetalle,
  PlanoForm,
  Producto,
  ProductoForm,
  Salon,
  Sector,
  TipoTurno,
  Turno,
} from './modelos';

/** Llamadas al backend. Devuelven promesas y los errores llegan como Error con un mensaje listo para mostrar. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  estadoIngreso = () => this.pedir(this.http.get<EstadoIngreso>('/api/auth/estado'));
  ingresar = (usuarioId: number, pin: string) =>
    this.pedir(this.http.post<SesionNueva>('/api/auth/ingresar', { usuarioId, pin }));
  crearPrimerUsuario = (nombre: string, pin: string) =>
    this.pedir(this.http.post<SesionNueva>('/api/auth/primer-usuario', { nombre, pin }));
  salir = () => this.pedir(this.http.post<void>('/api/auth/salir', {}));
  usuarios = () => this.pedir(this.http.get<Persona[]>('/api/usuarios'));
  crearUsuario = (nombre: string, pin: string) => this.pedir(this.http.post<Persona>('/api/usuarios', { nombre, pin }));
  cambiarPin = (id: number, pin: string) => this.pedir(this.http.put<void>(`/api/usuarios/${id}/pin`, { pin }));
  quitarUsuario = (id: number) => this.pedir(this.http.delete<void>(`/api/usuarios/${id}`));

  stock = () => this.pedir(this.http.get<Stock>('/api/stock'));
  alertasStock = () => this.pedir(this.http.get<AlertasStock>('/api/stock/alertas'));
  crearInsumo = (form: InsumoForm) => this.pedir(this.http.post<Insumo>('/api/stock/insumos', form));
  editarInsumo = (id: number, form: InsumoForm) => this.pedir(this.http.put<Insumo>(`/api/stock/insumos/${id}`, form));
  cargarCompra = (id: number, cantidad: number) =>
    this.pedir(this.http.post<Insumo>(`/api/stock/insumos/${id}/compra`, { cantidad }));
  registrarConteo = (id: number, cantidad: number) =>
    this.pedir(this.http.post<Insumo>(`/api/stock/insumos/${id}/conteo`, { cantidad }));
  quitarInsumo = (id: number) => this.pedir(this.http.delete<void>(`/api/stock/insumos/${id}`));
  guardarReceta = (productoId: number, lineas: { insumoId: number; cantidad: number }[]) =>
    this.pedir(this.http.put<void>(`/api/stock/recetas/${productoId}`, { lineas }));
  controlarPorUnidad = (productoId: number) =>
    this.pedir(this.http.post<Insumo>(`/api/stock/recetas/${productoId}/por-unidad`, {}));

  mesas = () => this.pedir(this.http.get<MesaResumen[]>('/api/mesas'));
  agregarMesa = (numero: number, sectorId?: number) =>
    this.pedir(this.http.post<MesaResumen>('/api/mesas', { numero, sectorId }));
  cambiarNumeroMesa = (id: number, numero: number) =>
    this.pedir(this.http.put<MesaResumen>(`/api/mesas/${id}/numero`, { numero }));
  pasarMesita = (origenId: number, destinoId: number) =>
    this.pedir(this.http.post<MesaResumen[]>(`/api/mesas/${origenId}/pasar-mesita`, { destinoId }));
  partirMesa = (id: number) => this.pedir(this.http.post<MesaResumen[]>(`/api/mesas/${id}/partir`, {}));
  quitarMesa = (id: number) => this.pedir(this.http.delete<void>(`/api/mesas/${id}`));
  abrirMesa = (id: number) => this.pedir(this.http.post<PedidoDetalle>(`/api/mesas/${id}/abrir`, {}));

  salon = () => this.pedir(this.http.get<Salon>('/api/salon'));
  guardarPlano = (form: PlanoForm) => this.pedir(this.http.put<Salon>('/api/salon', form));
  crearSector = (nombre: string) => this.pedir(this.http.post<Sector>('/api/salon/sectores', { nombre }));

  pedidosSinMesa = () => this.pedir(this.http.get<PedidoSinMesa[]>('/api/pedidos/sin-mesa'));
  abrirSinMesa = (tipo: TipoPedido, cliente: string) =>
    this.pedir(this.http.post<PedidoDetalle>('/api/pedidos/sin-mesa', { tipo, cliente }));
  marcarEntrega = (pedidoId: number, estado: EstadoEntrega) =>
    this.pedir(this.http.put<PedidoDetalle>(`/api/pedidos/${pedidoId}/entrega`, { estado }));
  marcarFacturado = (pedidoId: number, facturado: boolean) =>
    this.pedir(this.http.put<PedidoDetalle>(`/api/pedidos/${pedidoId}/facturado`, { facturado }));
  pasarA = (pedidoId: number, usuarioId: number) =>
    this.pedir(this.http.put<PedidoDetalle>(`/api/pedidos/${pedidoId}/atiende`, { usuarioId }));

  pedido = (id: number) => this.pedir(this.http.get<PedidoDetalle>(`/api/pedidos/${id}`));
  agregarItem = (pedidoId: number, productoId: number, cantidad = 1) =>
    this.pedir(this.http.post<PedidoDetalle>(`/api/pedidos/${pedidoId}/items`, { productoId, cantidad }));
  cambiarCantidad = (pedidoId: number, itemId: number, cantidad: number) =>
    this.pedir(this.http.put<PedidoDetalle>(`/api/pedidos/${pedidoId}/items/${itemId}`, { cantidad }));
  ponerNota = (pedidoId: number, itemId: number, nota: string, soloUna: boolean) =>
    this.pedir(this.http.put<PedidoDetalle>(`/api/pedidos/${pedidoId}/items/${itemId}/nota`, { nota, soloUna }));
  aplicarDescuento = (pedidoId: number, porcentaje: number) =>
    this.pedir(this.http.put<PedidoDetalle>(`/api/pedidos/${pedidoId}/descuento`, { porcentaje }));
  cobrar = (pedidoId: number, cobro: CobroForm) =>
    this.pedir(this.http.post<PedidoDetalle>(`/api/pedidos/${pedidoId}/pagos`, cobro));
  anularPago = (pedidoId: number, pagoId: number) =>
    this.pedir(this.http.delete<PedidoDetalle>(`/api/pedidos/${pedidoId}/pagos/${pagoId}`));

  caja = () => this.pedir(this.http.get<Caja>('/api/caja'));
  turno = (id: number) => this.pedir(this.http.get<Turno>(`/api/caja/turnos/${id}`));
  abrirTurno = (tipo: TipoTurno, responsable: string, fondoInicial: number) =>
    this.pedir(this.http.post<Turno>('/api/caja/turnos', { tipo, responsable, fondoInicial }));
  cerrarTurno = (id: number, efectivoContado: number, observaciones: string) =>
    this.pedir(this.http.post<Turno>(`/api/caja/turnos/${id}/cerrar`, { efectivoContado, observaciones }));

  cerrarPedido = (pedidoId: number) => this.pedir(this.http.post<PedidoDetalle>(`/api/pedidos/${pedidoId}/cerrar`, {}));

  carta = (soloActivos = true) => this.pedir(this.http.get<Categoria[]>('/api/carta', { params: { soloActivos } }));
  crearCategoria = (nombre: string) => this.pedir(this.http.post<Categoria>('/api/carta/categorias', { nombre }));
  crearProducto = (form: ProductoForm) => this.pedir(this.http.post<Producto>('/api/carta/productos', form));
  editarProducto = (id: number, form: ProductoForm) =>
    this.pedir(this.http.put<Producto>(`/api/carta/productos/${id}`, form));

  private async pedir<T>(llamada: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(llamada);
    } catch (error) {
      throw new Error(mensajeDeError(error));
    }
  }
}

const mensajeDeError = (error: unknown): string => {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Algo salió mal. Probá de nuevo.';
  }
  if (error.status === 0) {
    return 'No hay conexión con el sistema. Revisá el wifi y probá de nuevo.';
  }
  const detalle = error.error?.detail;
  return typeof detalle === 'string' && detalle ? detalle : 'Algo salió mal. Probá de nuevo.';
};
