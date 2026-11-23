import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { Categoria, MesaResumen, PedidoDetalle, Producto, ProductoForm } from './modelos';

/** Llamadas al backend. Devuelven promesas y los errores llegan como Error con un mensaje listo para mostrar. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  mesas = () => this.pedir(this.http.get<MesaResumen[]>('/api/mesas'));
  agregarMesa = (numero: number) => this.pedir(this.http.post<MesaResumen>('/api/mesas', { numero }));
  quitarMesa = (id: number) => this.pedir(this.http.delete<void>(`/api/mesas/${id}`));
  abrirMesa = (id: number) => this.pedir(this.http.post<PedidoDetalle>(`/api/mesas/${id}/abrir`, {}));

  pedido = (id: number) => this.pedir(this.http.get<PedidoDetalle>(`/api/pedidos/${id}`));
  agregarItem = (pedidoId: number, productoId: number, cantidad = 1) =>
    this.pedir(this.http.post<PedidoDetalle>(`/api/pedidos/${pedidoId}/items`, { productoId, cantidad }));
  cambiarCantidad = (pedidoId: number, itemId: number, cantidad: number) =>
    this.pedir(this.http.put<PedidoDetalle>(`/api/pedidos/${pedidoId}/items/${itemId}`, { cantidad }));
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
