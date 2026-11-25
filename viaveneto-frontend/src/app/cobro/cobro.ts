import { Component, OnInit, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe } from '../comun/formato';
import { DESCUENTO_EFECTIVO, MEDIOS, MEDIOS_LOCAL, nombreMedio } from '../comun/medios';
import { ItemPedido, MedioPago, PedidoDetalle } from '../comun/modelos';
import { abrirTicket } from '../ticket/abrir-ticket';

/** Cómo se arma el monto de este pago: todo lo que falta, en partes iguales o eligiendo los platos de cada uno. */
type Forma = 'todo' | 'partes' | 'platos';

const MAXIMO_PARTES = 20;

@Component({
  selector: 'app-cobro',
  imports: [PlataPipe, RouterLink],
  styleUrl: './cobro.css',
  templateUrl: './cobro.html',
})
export class Cobro implements OnInit {
  readonly id = input.required({ transform: numberAttribute });

  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly DESCUENTO_EFECTIVO = DESCUENTO_EFECTIVO;
  protected readonly nombreMedio = nombreMedio;

  protected readonly pedido = signal<PedidoDetalle | null>(null);
  protected readonly forma = signal<Forma>('todo');
  protected readonly partes = signal(2);
  /** Cuántas unidades de cada línea paga esta persona (forma "platos"). */
  protected readonly elegidos = signal<Record<number, number>>({});
  protected readonly monto = signal<number | null>(null);
  protected readonly medio = signal<MedioPago | null>(null);
  protected readonly recibido = signal<number | null>(null);
  protected readonly propina = signal<number | null>(null);
  protected readonly trabajando = signal(false);

  protected readonly titulo = computed(() => this.pedido()?.titulo ?? 'Pedido');
  /** Los de Rappi se cobran con Rappi; en el local, con los medios de siempre. */
  protected readonly medios = computed(() =>
    this.pedido()?.tipo === 'DELIVERY' ? MEDIOS.filter((m) => m.medio === 'RAPPI') : MEDIOS_LOCAL,
  );
  protected readonly saldo = computed(() => this.pedido()?.saldo ?? 0);
  protected readonly cobrada = computed(() => this.pedido()?.estado === 'CERRADO');
  protected readonly conDescuento = computed(() => (this.pedido()?.descuentoPorcentaje ?? 0) > 0);

  protected readonly aPagar = computed(() => (this.monto() ?? 0) + (this.propina() ?? 0));
  protected readonly vuelto = computed(() => {
    const recibido = this.recibido();
    return recibido === null ? null : recibido - this.aPagar();
  });
  /** Billetes con los que probablemente pague, para no tener que escribir. */
  protected readonly billetes = computed(() => {
    const total = this.aPagar();
    if (!total) return [];
    const redondeos = [1000, 10000, 20000].map((b) => Math.ceil(total / b) * b);
    return [...new Set([total, ...redondeos])].sort((a, b) => a - b);
  });
  protected readonly puedeCobrar = computed(() => {
    const monto = this.monto() ?? 0;
    const vuelto = this.vuelto();
    return !this.trabajando() && !!this.medio() && monto > 0 && monto <= this.saldo() && (vuelto === null || vuelto >= 0);
  });

  async ngOnInit() {
    try {
      this.actualizar(await this.api.pedido(this.id()));
      this.elegirForma('todo');
      if (this.pedido()?.tipo === 'DELIVERY') this.medio.set('RAPPI');
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  // ---------- Cuánto paga ----------

  protected elegirForma(forma: Forma) {
    this.forma.set(forma);
    this.elegidos.set({});
    this.recalcularMonto();
  }

  protected cambiarPartes(diferencia: number) {
    this.partes.update((n) => Math.min(MAXIMO_PARTES, Math.max(1, n + diferencia)));
    this.recalcularMonto();
  }

  protected elegidosDe(item: ItemPedido) {
    return this.elegidos()[item.id] ?? 0;
  }

  protected cambiarElegido(item: ItemPedido, diferencia: number) {
    const cantidad = Math.min(item.cantidad, Math.max(0, this.elegidosDe(item) + diferencia));
    this.elegidos.update((actual) => ({ ...actual, [item.id]: cantidad }));
    this.recalcularMonto();
  }

  protected escribirMonto(valor: string) {
    this.monto.set(valor === '' ? null : Number(valor));
  }

  // ---------- Con qué paga ----------

  /** "con débito", "con efectivo", pero "con Rappi" (es un nombre propio). */
  protected medioEnTexto(medio: MedioPago) {
    return medio === 'RAPPI' ? 'Rappi' : nombreMedio(medio).toLowerCase();
  }

  protected elegirMedio(medio: MedioPago) {
    this.medio.set(medio);
    if (medio !== 'EFECTIVO') this.recibido.set(null);
  }

  protected escribirRecibido(valor: string) {
    this.recibido.set(valor === '' ? null : Number(valor));
  }

  protected escribirPropina(valor: string) {
    this.propina.set(valor === '' ? null : Number(valor));
  }

  protected propinaDiezPorCiento() {
    this.propina.set(Math.round((this.monto() ?? 0) * 0.1));
  }

  protected async cambiarDescuento() {
    await this.trabajar(async () => {
      this.actualizar(await this.api.aplicarDescuento(this.id(), this.conDescuento() ? 0 : DESCUENTO_EFECTIVO));
      this.recalcularMonto();
    });
  }

  // ---------- Cobrar ----------

  protected async cobrar() {
    const medio = this.medio();
    const monto = this.monto();
    if (!medio || !monto) return;
    await this.trabajar(async () => {
      const actualizado = await this.api.cobrar(this.id(), {
        medio,
        monto,
        propina: this.propina() ?? 0,
        recibido: medio === 'EFECTIVO' ? this.recibido() : null,
      });
      this.actualizar(actualizado);
      const vuelto = this.vuelto();
      this.avisos.ok(
        vuelto ? `Cobrado. Vuelto: $ ${vuelto.toLocaleString('es-AR')}` : `Cobrado con ${this.medioEnTexto(medio)}`,
      );
      this.prepararSiguientePago();
    });
  }

  protected async borrarPago(pagoId: number) {
    if (!confirm('¿Borrar este pago? Usalo solo si se cargó mal.')) return;
    await this.trabajar(async () => {
      this.actualizar(await this.api.anularPago(this.id(), pagoId));
      this.recalcularMonto();
    });
  }

  protected imprimirTicket() {
    abrirTicket(this.id());
  }

  private prepararSiguientePago() {
    this.medio.set(this.pedido()?.tipo === 'DELIVERY' ? 'RAPPI' : null);
    this.recibido.set(null);
    this.propina.set(null);
    if (this.forma() === 'partes') {
      // Una parte menos por pagar: la última se lleva lo que quede por redondeo.
      this.partes.update((n) => Math.max(1, n - 1));
    }
    this.elegidos.set({});
    this.recalcularMonto();
  }

  private recalcularMonto() {
    const saldo = this.saldo();
    const pedido = this.pedido();
    if (!pedido || saldo <= 0) {
      this.monto.set(null);
      return;
    }
    switch (this.forma()) {
      case 'todo':
        this.monto.set(saldo);
        break;
      case 'partes':
        this.monto.set(this.partes() <= 1 ? saldo : Math.round(saldo / this.partes()));
        break;
      case 'platos': {
        const bruto = pedido.items.reduce((suma, i) => suma + this.elegidosDe(i) * i.precioUnitario, 0);
        const conDescuento = Math.round(bruto * (1 - pedido.descuentoPorcentaje / 100));
        this.monto.set(conDescuento ? Math.min(conDescuento, saldo) : null);
        break;
      }
    }
  }

  private actualizar(pedido: PedidoDetalle) {
    this.pedido.set(pedido);
  }

  private async trabajar(accion: () => Promise<void>) {
    this.trabajando.set(true);
    try {
      await accion();
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.trabajando.set(false);
    }
  }
}
