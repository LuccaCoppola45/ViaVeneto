import { Component, OnInit, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe } from '../comun/formato';
import { nombreMedio } from '../comun/medios';
import { PedidoDetalle } from '../comun/modelos';

/** Ticket no fiscal de una mesa. Si todavía no se cobró todo, sale como PRECUENTA. */
@Component({
  selector: 'app-ticket',
  imports: [PlataPipe, DatePipe],
  styleUrl: './ticket.css',
  template: `
    @if (pedido(); as p) {
      <div class="acciones no-imprimir">
        <button class="primario" (click)="imprimir()">🖨️ Imprimir</button>
        <button (click)="cerrarVentana()">Cerrar</button>
      </div>
      <div class="papel">
        <div class="centro marca">VIA VENETO</div>
        <div class="centro">{{ esPrecuenta() ? 'PRECUENTA' : 'TICKET' }}</div>
        <div class="corte"></div>
        <div class="linea"><span class="grande">{{ p.titulo }}</span><span>#{{ p.id }}</span></div>
        @if (p.atiendeNombre) { <div>Atendió: {{ p.atiendeNombre }}</div> }
        <div>{{ (p.cerradoEn ?? ahora) | date: 'dd/MM/yyyy HH:mm' }}</div>
        <div class="corte"></div>
        @for (item of p.items; track item.id) {
          <div class="linea"><span>{{ item.cantidad }} x {{ item.nombre }}</span><span>{{ item.subtotal | plata }}</span></div>
          @if (item.nota) { <div class="nota">{{ item.nota }}</div> }
        }
        <div class="corte"></div>
        @if (p.descuento) {
          <div class="linea"><span>Subtotal</span><span>{{ p.subtotal | plata }}</span></div>
          <div class="linea"><span>Descuento {{ p.descuentoPorcentaje }}%</span><span>-{{ p.descuento | plata }}</span></div>
        }
        <div class="linea grande"><span>TOTAL</span><span>{{ p.total | plata }}</span></div>
        @if (p.pagos.length) {
          <div class="corte"></div>
          @for (pago of p.pagos; track pago.id) {
            <div class="linea"><span>{{ nombreMedio(pago.medio) }}</span><span>{{ pago.monto | plata }}</span></div>
            @if (pago.propina) { <div class="linea sub"><span>Propina</span><span>{{ pago.propina | plata }}</span></div> }
            @if (pago.recibido) {
              <div class="linea sub"><span>Recibido</span><span>{{ pago.recibido | plata }}</span></div>
              <div class="linea sub"><span>Vuelto</span><span>{{ pago.vuelto | plata }}</span></div>
            }
          }
          @if (p.saldo) { <div class="linea grande"><span>FALTA</span><span>{{ p.saldo | plata }}</span></div> }
        }
        <div class="corte"></div>
        <div class="centro">¡Gracias por su visita!</div>
        <div class="centro chico">Documento no válido como factura</div>
      </div>
    } @else {
      <p class="cargando">Cargando ticket…</p>
    }
  `,
})
export class TicketPedido implements OnInit {
  readonly id = input.required({ transform: numberAttribute });
  readonly imprimirAlAbrir = input(false, { alias: 'imprimir', transform: (v: unknown) => v === '1' || v === true });

  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly pedido = signal<PedidoDetalle | null>(null);
  protected readonly esPrecuenta = computed(() => (this.pedido()?.saldo ?? 0) > 0);
  protected readonly nombreMedio = nombreMedio;
  protected readonly ahora = new Date().toISOString();

  async ngOnInit() {
    try {
      this.pedido.set(await this.api.pedido(this.id()));
      if (this.imprimirAlAbrir()) {
        setTimeout(() => this.imprimir(), 300);
      }
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  protected imprimir() {
    window.print();
  }

  protected cerrarVentana() {
    window.close();
    history.back();
  }
}
