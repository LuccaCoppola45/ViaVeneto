import { Component, OnInit, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe } from '../comun/formato';
import { Categoria, EstadoEntrega, ItemPedido, PedidoDetalle, Persona, Producto } from '../comun/modelos';
import { NOMBRE_ENTREGA } from '../comun/medios';
import { refrescarCadaRato } from '../comun/refresco';
import { abrirTicket } from '../ticket/abrir-ticket';

type Vista = 'carta' | 'cuenta';

const NOTAS_RAPIDAS = ['Sin sal', 'Sin cebolla', 'Sin TACC', 'Jugosa', 'A punto', 'Bien cocida', 'Para compartir', 'Aparte'];

@Component({
  selector: 'app-pedido',
  imports: [PlataPipe, RouterLink],
  styleUrl: './pedido.css',
  templateUrl: './pedido.html',
})
export class PedidoPagina implements OnInit {
  readonly id = input.required({ transform: numberAttribute });

  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);

  protected readonly pedido = signal<PedidoDetalle | null>(null);
  protected readonly carta = signal<Categoria[]>([]);
  protected readonly categoriaElegidaId = signal<number | null>(null);
  protected readonly vista = signal<Vista>('carta');
  protected readonly confirmandoCierre = signal(false);
  protected readonly cerrando = signal(false);
  protected readonly editandoNota = signal<ItemPedido | null>(null);
  protected readonly textoNota = signal('');
  protected readonly NOTAS_RAPIDAS = NOTAS_RAPIDAS;
  /** Con esta cantidad o menos, el botón del plato avisa cuántos quedan. */
  protected readonly POCOS = 5;

  protected readonly titulo = computed(() => this.pedido()?.titulo ?? 'Pedido');
  protected readonly sinMesa = computed(() => !!this.pedido() && this.pedido()!.tipo !== 'MESA');
  protected readonly personas = signal<Persona[]>([]);
  protected readonly NOMBRE_ENTREGA = NOMBRE_ENTREGA;
  protected readonly ENTREGAS: EstadoEntrega[] = ['PREPARANDO', 'LISTO', 'ENTREGADO'];
  protected readonly abierto = computed(() => this.pedido()?.estado === 'ABIERTO');
  protected readonly unidades = computed(() => this.pedido()?.items.reduce((suma, i) => suma + i.cantidad, 0) ?? 0);
  protected readonly categoriaElegida = computed(
    () => this.carta().find((c) => c.id === this.categoriaElegidaId()) ?? this.carta()[0],
  );
  private readonly cantidadPorProducto = computed(
    () => new Map(this.pedido()?.items.map((i) => [i.productoId, i.cantidad]) ?? []),
  );

  /** Las modificaciones se mandan de a una, en orden, para que tocar rápido varias veces no mezcle respuestas. */
  private cola: Promise<unknown> = Promise.resolve();
  private pendientes = 0;

  constructor() {
    refrescarCadaRato(() => this.refrescar());
  }

  ngOnInit() {
    this.cargarTodo();
  }

  protected cantidadDe(producto: Producto) {
    return this.cantidadPorProducto().get(producto.id) ?? 0;
  }

  protected agregar(producto: Producto) {
    this.modificar(() => this.api.agregarItem(this.id(), producto.id));
  }

  protected cambiar(item: ItemPedido, diferencia: number) {
    this.modificar(() => this.api.cambiarCantidad(this.id(), item.id, item.cantidad + diferencia));
  }

  protected abrirNota(item: ItemPedido) {
    this.textoNota.set(item.nota ?? '');
    this.editandoNota.set(item);
  }

  protected sumarNotaRapida(rapida: string) {
    const actual = this.textoNota().trim();
    this.textoNota.set(actual ? `${actual}, ${rapida.toLowerCase()}` : rapida);
  }

  protected guardarNota(item: ItemPedido, soloUna: boolean) {
    const nota = this.textoNota();
    this.editandoNota.set(null);
    this.modificar(() => this.api.ponerNota(this.id(), item.id, nota, soloUna));
  }

  protected imprimirPrecuenta() {
    abrirTicket(this.id());
  }

  protected imprimirTicket() {
    abrirTicket(this.id());
  }

  protected async marcarFacturado(facturado: boolean) {
    try {
      this.pedido.set(await this.api.marcarFacturado(this.id(), facturado));
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  /** Cuando una moza le pasa la mesa a otra. */
  protected pasarA(usuarioId: string) {
    this.modificar(() => this.api.pasarA(this.id(), Number(usuarioId)));
  }

  protected async marcarEntrega(estado: EstadoEntrega) {
    const pedido = this.pedido();
    if (estado === 'ENTREGADO' && pedido && pedido.saldo > 0) {
      this.avisos.error('Antes de entregarlo falta cobrar.');
      this.router.navigate(['/pedidos', this.id(), 'cobrar']);
      return;
    }
    this.modificar(() => this.api.marcarEntrega(this.id(), estado));
    if (estado === 'ENTREGADO') {
      this.avisos.ok(`${this.titulo()} entregado`);
      this.router.navigate(['/mesas']);
    }
  }

  protected async cerrar() {
    this.cerrando.set(true);
    await this.cola;
    try {
      const cerrado = await this.api.cerrarPedido(this.id());
      const nombre = this.titulo();
      this.avisos.ok(
        this.sinMesa() ? `${nombre}: cancelado` : cerrado.estado === 'ANULADO' ? `${nombre} liberada` : `${nombre} cerrada`,
      );
      this.router.navigate(['/mesas']);
    } catch (error) {
      this.avisos.error((error as Error).message);
      this.cerrando.set(false);
      this.confirmandoCierre.set(false);
    }
  }

  private modificar(llamada: () => Promise<PedidoDetalle>) {
    this.pendientes++;
    this.cola = this.cola.then(async () => {
      try {
        this.pedido.set(await llamada());
        this.carta.set(await this.api.carta());
      } catch (error) {
        this.avisos.error((error as Error).message);
        await this.refrescar(true);
      } finally {
        this.pendientes--;
      }
    });
  }

  private async cargarTodo() {
    try {
      const [pedido, carta, personas] = await Promise.all([this.api.pedido(this.id()), this.api.carta(), this.api.usuarios()]);
      this.pedido.set(pedido);
      this.carta.set(carta);
      this.personas.set(personas);
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  /** No pisa la pantalla mientras hay cambios en camino. */
  private async refrescar(forzar = false) {
    if (!forzar && (this.pendientes > 0 || this.cerrando())) return;
    try {
      const [pedido, carta] = await Promise.all([this.api.pedido(this.id()), this.api.carta()]);
      this.pedido.set(pedido);
      this.carta.set(carta);
    } catch {
      // Si falla el refresco automático no molestamos: el próximo lo reintenta.
    }
  }
}
