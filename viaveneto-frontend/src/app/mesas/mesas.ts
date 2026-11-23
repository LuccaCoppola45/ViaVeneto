import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe, tiempoDesde } from '../comun/formato';
import { MesaResumen } from '../comun/modelos';
import { refrescarCadaRato } from '../comun/refresco';

@Component({
  selector: 'app-mesas',
  imports: [PlataPipe],
  styleUrl: './mesas.css',
  template: `
    <div class="encabezado">
      <h1>Mesas</h1>
      @if (mesas().length) {
        <div class="contadores">
          <span class="chip libre">{{ libres() }} libres</span>
          <span class="chip ocupada">{{ ocupadas() }} ocupadas</span>
        </div>
      }
    </div>

    @if (cargando()) {
      <p class="cargando">Cargando mesas…</p>
    } @else if (!mesas().length) {
      <p class="cargando">Todavía no hay mesas. Agregalas desde “Carta y mesas”.</p>
    }

    <div class="plano">
      @for (mesa of mesas(); track mesa.id) {
        <button
          class="mesa"
          [class.ocupada]="mesa.ocupada"
          [disabled]="abriendo() === mesa.id"
          (click)="entrar(mesa)"
          [attr.aria-label]="'Mesa ' + mesa.numero + (mesa.ocupada ? ', ocupada' : ', libre')"
        >
          <span class="numero">{{ mesa.numero }}</span>
          @if (mesa.ocupada) {
            <span class="total">{{ mesa.total | plata }}</span>
            <span class="detalle">{{ mesa.cantidadItems }} u. · {{ hace(mesa) }}</span>
          } @else {
            <span class="detalle">Libre</span>
          }
        </button>
      }
    </div>
  `,
})
export class Mesas {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);

  protected readonly mesas = signal<MesaResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly abriendo = signal<number | null>(null);
  private readonly ahora = signal(Date.now());

  protected readonly ocupadas = computed(() => this.mesas().filter((m) => m.ocupada).length);
  protected readonly libres = computed(() => this.mesas().length - this.ocupadas());

  constructor() {
    this.cargar();
    refrescarCadaRato(() => this.cargar());
  }

  protected hace(mesa: MesaResumen) {
    return mesa.abiertaDesde ? tiempoDesde(mesa.abiertaDesde, this.ahora()) : '';
  }

  /** Mesa libre: la abre. Mesa ocupada: muestra su pedido. */
  protected async entrar(mesa: MesaResumen) {
    if (mesa.pedidoId) {
      this.router.navigate(['/pedidos', mesa.pedidoId]);
      return;
    }
    this.abriendo.set(mesa.id);
    try {
      const pedido = await this.api.abrirMesa(mesa.id);
      this.router.navigate(['/pedidos', pedido.id]);
    } catch (error) {
      this.avisos.error((error as Error).message);
      this.abriendo.set(null);
    }
  }

  private async cargar() {
    try {
      this.mesas.set(await this.api.mesas());
      this.ahora.set(Date.now());
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.cargando.set(false);
    }
  }
}
