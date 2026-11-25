import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe, tiempoDesde } from '../comun/formato';
import { MesaResumen, Salon, Sector } from '../comun/modelos';
import { refrescarCadaRato } from '../comun/refresco';
import { ELEMENTOS, MesaDibujo, estiloPieza, estiloPlano, tamanioMesa } from '../salon/dibujo';
import { sectorElegido } from '../salon/sector-elegido';
import { Sesion } from '../comun/sesion';
import { SinMesa } from './sin-mesa';

const FINO = 50;
const CLAVE_MIS_MESAS = 'viaveneto.misMesas';

const leerMisMesas = () => {
  try {
    return localStorage.getItem(CLAVE_MIS_MESAS) === '1';
  } catch {
    return false;
  }
};

/**
 * Juntar o separar mesitas, paso a paso:
 * primero se toca la mesa a cambiar, después se elige qué hacer
 * y, si es pasar una mesita, se toca la mesa que la recibe.
 */
type Arreglo =
  | { paso: 'elegir' }
  | { paso: 'opciones'; mesaId: number }
  | { paso: 'destino'; mesaId: number };

@Component({
  selector: 'app-mesas',
  imports: [PlataPipe, MesaDibujo, SinMesa],
  styleUrl: './mesas.css',
  templateUrl: './mesas.html',
})
export class Mesas {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  private readonly sector = sectorElegido();
  private readonly sesion = inject(Sesion);

  /** Resalta las mesas de quien está usando la tablet, como su mitad de la libreta. */
  protected readonly misMesas = signal(leerMisMesas());

  protected readonly salon = signal<Salon | null>(null);
  protected readonly mesas = signal<MesaResumen[]>([]);
  protected readonly abriendo = signal<number | null>(null);
  protected readonly arreglo = signal<Arreglo | null>(null);
  protected readonly trabajando = signal(false);
  private readonly ahora = signal(Date.now());

  protected readonly ELEMENTOS = ELEMENTOS;

  protected readonly sectorActual = computed<Sector | undefined>(() => {
    const sectores = this.salon()?.sectores ?? [];
    return sectores.find((s) => s.id === this.sector.elegido()) ?? sectores[0];
  });

  /** Las mesas sin sector (creadas antes de que existiera el plano) se muestran en el primero. */
  private readonly sectorDeMesa = (mesa: MesaResumen) => mesa.sectorId ?? this.salon()?.sectores[0]?.id;

  protected readonly mesasDelSector = computed(() =>
    this.mesas().filter((m) => this.sectorDeMesa(m) === this.sectorActual()?.id),
  );

  protected readonly mesaArreglada = computed(() => {
    const arreglo = this.arreglo();
    return arreglo && arreglo.paso !== 'elegir' ? this.mesas().find((m) => m.id === arreglo.mesaId) : undefined;
  });

  constructor() {
    this.cargarTodo();
    refrescarCadaRato(() => this.cargarMesas());
  }

  protected cambiarMisMesas() {
    this.misMesas.update((v) => !v);
    try {
      localStorage.setItem(CLAVE_MIS_MESAS, this.misMesas() ? '1' : '0');
    } catch {
      // Sin almacenamiento: se recuerda solo mientras esté abierta la pantalla.
    }
  }

  protected esMia(mesa: MesaResumen) {
    return mesa.ocupada && mesa.atiendeId === this.sesion.usuario()?.id;
  }

  protected esAjena(mesa: MesaResumen) {
    return this.misMesas() && mesa.ocupada && !this.esMia(mesa);
  }

  protected elegirSector(sector: Sector) {
    this.sector.elegir(sector.id);
  }

  protected ocupadasEn(sector: Sector) {
    return this.mesas().filter((m) => m.ocupada && this.sectorDeMesa(m) === sector.id).length;
  }

  protected estiloMesa(mesa: MesaResumen) {
    const medidas = this.salon()!.medidas;
    const { ancho, alto } = tamanioMesa(medidas, mesa.vertical, mesa.mesitas);
    return estiloPieza(medidas, mesa.x, mesa.y, ancho, alto);
  }

  protected estiloPlano() {
    return estiloPlano(this.salon()!.medidas);
  }

  protected estiloElemento(e: { x: number; y: number; ancho: number; alto: number }) {
    return estiloPieza(this.salon()!.medidas, e.x, e.y, e.ancho, e.alto);
  }

  protected esFino(e: { ancho: number; alto: number }) {
    return Math.min(e.ancho, e.alto) < FINO;
  }

  protected hace(mesa: MesaResumen) {
    return mesa.abiertaDesde ? tiempoDesde(mesa.abiertaDesde, this.ahora()) : '';
  }

  protected tocarMesa(mesa: MesaResumen) {
    const arreglo = this.arreglo();
    if (!arreglo) {
      this.entrar(mesa);
    } else if (arreglo.paso === 'destino') {
      this.pasarMesita(arreglo.mesaId, mesa);
    } else {
      this.arreglo.set({ paso: 'opciones', mesaId: mesa.id });
    }
  }

  protected empezarArreglo() {
    this.arreglo.set({ paso: 'elegir' });
  }

  protected terminarArreglo() {
    this.arreglo.set(null);
  }

  protected elegirDestino(mesa: MesaResumen) {
    this.arreglo.set({ paso: 'destino', mesaId: mesa.id });
  }

  /** Se puede sacar una mesita si le queda otra, o si la mesa está libre (y desaparece). */
  protected puedeDarMesita(mesa: MesaResumen) {
    return mesa.mesitas > 1 || !mesa.ocupada;
  }

  protected async partir(mesa: MesaResumen) {
    await this.arreglar(async () => {
      this.mesas.set(await this.api.partirMesa(mesa.id));
      const nueva = this.mesas().find((m) => m.numero === mesa.numero && m.id !== mesa.id && m.etiqueta !== mesa.etiqueta);
      this.avisos.ok(nueva ? `Listo: ahora están la ${mesa.etiqueta} y la ${nueva.etiqueta}` : 'Mesa partida');
    });
  }

  private async pasarMesita(origenId: number, destino: MesaResumen) {
    if (origenId === destino.id) {
      this.avisos.error('Tocá otra mesa: la que recibe la mesita.');
      return;
    }
    const origen = this.mesas().find((m) => m.id === origenId);
    await this.arreglar(async () => {
      this.mesas.set(await this.api.pasarMesita(origenId, destino.id));
      const lugares = (destino.mesitas + 1) * 2;
      const seJunto = origen && origen.mesitas === 1;
      this.avisos.ok(
        seJunto
          ? `La ${origen.etiqueta} se juntó con la ${destino.etiqueta}: ahora es para ${lugares}`
          : `La ${destino.etiqueta} ahora es para ${lugares} personas`,
      );
    });
  }

  private async arreglar(accion: () => Promise<void>) {
    this.trabajando.set(true);
    try {
      await accion();
      this.arreglo.set({ paso: 'elegir' });
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.trabajando.set(false);
    }
  }

  /** Mesa libre: la abre. Mesa ocupada: muestra su pedido. */
  private async entrar(mesa: MesaResumen) {
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

  private async cargarTodo() {
    try {
      const [salon, mesas] = await Promise.all([this.api.salon(), this.api.mesas()]);
      this.salon.set(salon);
      this.mesas.set(mesas);
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  private async cargarMesas() {
    try {
      this.mesas.set(await this.api.mesas());
      this.ahora.set(Date.now());
    } catch {
      // El próximo refresco lo reintenta; no llenamos la pantalla de avisos.
    }
  }
}
