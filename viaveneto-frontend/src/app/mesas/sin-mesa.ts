import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe, tiempoDesde } from '../comun/formato';
import { NOMBRE_ENTREGA } from '../comun/medios';
import { EstadoEntrega, PedidoSinMesa, TipoPedido } from '../comun/modelos';
import { refrescarCadaRato } from '../comun/refresco';

/**
 * La columna "Para llevar y Rappi" de la pantalla de mesas: lo que antes se anotaba
 * en la libreta con la marca "para llevar". Cada pedido queda hasta que se retira.
 */
@Component({
  selector: 'app-sin-mesa',
  imports: [PlataPipe],
  styleUrl: './sin-mesa.css',
  template: `
    <section class="columna" aria-label="Para llevar y Rappi">
      <h2>Para llevar y Rappi</h2>
      <div class="nuevos">
        <button class="primario" (click)="empezar('PARA_LLEVAR')">🥡 Para llevar</button>
        <button class="primario rappi" (click)="empezar('DELIVERY')">🛵 Rappi</button>
      </div>

      @for (pedido of pedidos(); track pedido.id) {
        <div class="pedido" [attr.data-entrega]="pedido.entrega" (click)="abrir(pedido)">
          <div class="arriba">
            <strong>{{ pedido.titulo }}</strong>
            <span class="estado">{{ NOMBRE_ENTREGA[pedido.entrega] }}</span>
          </div>
          <div class="abajo">
            <span>{{ pedido.unidades }} u. · {{ pedido.total | plata }} · {{ hace(pedido) }}</span>
            @if (pedido.saldo > 0 && pedido.unidades) { <span class="falta">Falta cobrar</span> }
            @else if (pedido.estado === 'CERRADO') { <span class="pagado">Pagado ✓</span> }
          </div>
          @if (pedido.entrega === 'PREPARANDO' && pedido.unidades) {
            <button (click)="marcar($event, pedido, 'LISTO')">✔ Está listo</button>
          } @else if (pedido.entrega === 'LISTO') {
            <button (click)="marcar($event, pedido, 'ENTREGADO')">
              {{ pedido.tipo === 'DELIVERY' ? 'Se lo llevó Rappi' : 'Lo retiraron' }}
            </button>
          }
        </div>
      } @empty {
        <p class="vacio">No hay pedidos para llevar ni de Rappi.</p>
      }
    </section>

    @if (tipoNuevo(); as tipo) {
      <div class="fondo-modal" (click)="tipoNuevo.set(null)">
        <div class="modal" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
          <p><strong>{{ tipo === 'DELIVERY' ? 'Pedido de Rappi' : 'Pedido para llevar' }}</strong></p>
          <label class="campo">{{ tipo === 'DELIVERY' ? 'Número de pedido o nombre (opcional)' : '¿A nombre de quién?' }}
            <input [value]="cliente()" (input)="cliente.set($any($event.target).value)" maxlength="60"
                   (keydown.enter)="crear(tipo)" autofocus />
          </label>
          <div class="acciones">
            <button (click)="tipoNuevo.set(null)">Cancelar</button>
            <button class="primario" [disabled]="trabajando()" (click)="crear(tipo)">Empezar a cargar</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class SinMesa {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);

  protected readonly NOMBRE_ENTREGA = NOMBRE_ENTREGA;
  protected readonly pedidos = signal<PedidoSinMesa[]>([]);
  protected readonly tipoNuevo = signal<TipoPedido | null>(null);
  protected readonly cliente = signal('');
  protected readonly trabajando = signal(false);
  private readonly ahora = signal(Date.now());

  constructor() {
    this.cargar();
    refrescarCadaRato(() => this.cargar());
  }

  protected hace(pedido: PedidoSinMesa) {
    return tiempoDesde(pedido.abiertoEn, this.ahora());
  }

  protected empezar(tipo: TipoPedido) {
    this.cliente.set('');
    this.tipoNuevo.set(tipo);
  }

  protected async crear(tipo: TipoPedido) {
    if (tipo === 'PARA_LLEVAR' && !this.cliente().trim()) {
      this.avisos.error('Poné a nombre de quién es, así lo llaman cuando está listo.');
      return;
    }
    this.trabajando.set(true);
    try {
      const pedido = await this.api.abrirSinMesa(tipo, this.cliente());
      this.tipoNuevo.set(null);
      this.router.navigate(['/pedidos', pedido.id]);
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.trabajando.set(false);
    }
  }

  protected abrir(pedido: PedidoSinMesa) {
    this.router.navigate(['/pedidos', pedido.id]);
  }

  protected async marcar(evento: Event, pedido: PedidoSinMesa, estado: EstadoEntrega) {
    evento.stopPropagation();
    if (estado === 'ENTREGADO' && pedido.saldo > 0) {
      this.avisos.error(`Antes de entregarlo falta cobrar ${pedido.titulo}.`);
      this.router.navigate(['/pedidos', pedido.id, 'cobrar']);
      return;
    }
    try {
      await this.api.marcarEntrega(pedido.id, estado);
      await this.cargar();
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  private async cargar() {
    try {
      this.pedidos.set(await this.api.pedidosSinMesa());
      this.ahora.set(Date.now());
    } catch {
      // El próximo refresco lo reintenta.
    }
  }
}
