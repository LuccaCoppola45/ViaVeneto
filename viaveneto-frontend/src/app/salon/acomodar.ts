import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { Elemento, Medidas, MesaResumen, Salon, Sector, TipoElemento } from '../comun/modelos';
import { ELEMENTOS, MesaDibujo, TIPOS_ELEMENTO, estiloPieza, estiloPlano, tamanioMesa } from './dibujo';
import { sectorElegido } from './sector-elegido';

type MesaEnPlano = Pick<MesaResumen, 'id' | 'numero' | 'etiqueta' | 'mesitas' | 'ocupada' | 'x' | 'y' | 'vertical'> & {
  sectorId: number;
};
type ElementoEnPlano = Elemento & { clave: number; sectorId: number };
type Seleccion = { tipo: 'mesa'; id: number } | { tipo: 'elemento'; clave: number } | null;

type Arrastre = {
  modo: 'mover' | 'estirar';
  pieza: MesaEnPlano | ElementoEnPlano;
  desdeX: number;
  desdeY: number;
  original: { x: number; y: number; ancho: number; alto: number };
  seMovio: boolean;
};

/** Las piezas se acomodan de a 10 unidades para que quede todo alineado sin esfuerzo. */
const GRILLA = 10;
/** Cuántos píxeles hay que mover el dedo para que cuente como arrastre y no como toque. */
const UMBRAL_ARRASTRE = 6;
const FINO = 50;
const MINIMO_ELEMENTO = 20;

const ajustar = (valor: number) => Math.round(valor / GRILLA) * GRILLA;
const limitar = (valor: number, minimo: number, maximo: number) => Math.min(Math.max(valor, minimo), maximo);

@Component({
  selector: 'app-acomodar',
  imports: [MesaDibujo, RouterLink],
  styleUrl: './acomodar.css',
  templateUrl: './acomodar.html',
})
export class AcomodarSalon {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly sector = sectorElegido();
  private readonly plano = viewChild<ElementRef<HTMLElement>>('plano');

  protected readonly ELEMENTOS = ELEMENTOS;
  protected readonly TIPOS = TIPOS_ELEMENTO;

  protected readonly medidas = signal<Medidas | null>(null);
  protected readonly sectores = signal<Sector[]>([]);
  protected readonly mesas = signal<MesaEnPlano[]>([]);
  protected readonly elementos = signal<ElementoEnPlano[]>([]);
  protected readonly seleccion = signal<Seleccion>(null);
  readonly cambios = signal(false);
  protected readonly guardando = signal(false);

  private arrastre: Arrastre | null = null;
  private siguienteClave = 1;

  protected readonly sectorActual = computed(
    () => this.sectores().find((s) => s.id === this.sector.elegido()) ?? this.sectores()[0],
  );
  protected readonly mesasDelSector = computed(() => this.mesas().filter((m) => m.sectorId === this.sectorActual()?.id));
  protected readonly elementosDelSector = computed(() =>
    this.elementos().filter((e) => e.sectorId === this.sectorActual()?.id),
  );
  protected readonly mesaElegida = computed(() => {
    const sel = this.seleccion();
    return sel?.tipo === 'mesa' ? this.mesas().find((m) => m.id === sel.id) : undefined;
  });
  protected readonly elementoElegido = computed(() => {
    const sel = this.seleccion();
    return sel?.tipo === 'elemento' ? this.elementos().find((e) => e.clave === sel.clave) : undefined;
  });
  protected readonly otrosSectores = computed(() => this.sectores().filter((s) => s.id !== this.sectorActual()?.id));

  constructor() {
    this.cargar();
  }

  protected elegirSector(sector: Sector) {
    this.sector.elegir(sector.id);
    this.seleccion.set(null);
  }

  protected estiloMesa(mesa: MesaEnPlano) {
    const medidas = this.medidas()!;
    const { ancho, alto } = tamanioMesa(medidas, mesa.vertical, mesa.mesitas);
    return estiloPieza(medidas, mesa.x, mesa.y, ancho, alto);
  }

  protected estiloPlano() {
    return estiloPlano(this.medidas()!);
  }

  protected estiloElemento(e: ElementoEnPlano) {
    return estiloPieza(this.medidas()!, e.x, e.y, e.ancho, e.alto);
  }

  /** El botón ↻ va pegado a la esquina de arriba a la derecha de lo elegido. */
  protected estiloGirar(pieza: { left: string; top: string; width: string }) {
    return { left: `calc(${pieza.left} + ${pieza.width})`, top: pieza.top };
  }

  protected esFino(e: Elemento) {
    return Math.min(e.ancho, e.alto) < FINO;
  }

  protected esMesaElegida(mesa: MesaEnPlano) {
    return this.mesaElegida()?.id === mesa.id;
  }

  protected esElementoElegido(e: ElementoEnPlano) {
    return this.elementoElegido()?.clave === e.clave;
  }

  // ---------- Arrastrar con el dedo o el mouse ----------

  protected empezarArrastre(evento: PointerEvent, pieza: MesaEnPlano | ElementoEnPlano, modo: Arrastre['modo'] = 'mover') {
    evento.preventDefault();
    evento.stopPropagation();
    (evento.currentTarget as HTMLElement).setPointerCapture(evento.pointerId);
    const { ancho, alto } = this.tamanio(pieza);
    this.arrastre = {
      modo,
      pieza,
      desdeX: evento.clientX,
      desdeY: evento.clientY,
      original: { x: pieza.x, y: pieza.y, ancho, alto },
      seMovio: false,
    };
    this.seleccion.set('clave' in pieza ? { tipo: 'elemento', clave: pieza.clave } : { tipo: 'mesa', id: pieza.id });
  }

  protected moverArrastre(evento: PointerEvent) {
    const arrastre = this.arrastre;
    const plano = this.plano()?.nativeElement;
    const medidas = this.medidas();
    if (!arrastre || !plano || !medidas) return;

    const pxX = evento.clientX - arrastre.desdeX;
    const pxY = evento.clientY - arrastre.desdeY;
    if (!arrastre.seMovio && Math.hypot(pxX, pxY) < UMBRAL_ARRASTRE) return;
    arrastre.seMovio = true;

    const escala = medidas.ancho / plano.getBoundingClientRect().width;
    const dx = pxX * escala;
    const dy = pxY * escala;
    const { original, pieza } = arrastre;

    if (arrastre.modo === 'estirar' && 'clave' in pieza) {
      this.actualizarElemento(pieza.clave, {
        ancho: limitar(ajustar(original.ancho + dx), MINIMO_ELEMENTO, medidas.ancho - pieza.x),
        alto: limitar(ajustar(original.alto + dy), MINIMO_ELEMENTO, medidas.alto - pieza.y),
      });
      return;
    }
    const x = limitar(ajustar(original.x + dx), 0, medidas.ancho - original.ancho);
    const y = limitar(ajustar(original.y + dy), 0, medidas.alto - original.alto);
    if ('clave' in pieza) {
      this.actualizarElemento(pieza.clave, { x, y });
    } else {
      this.actualizarMesa(pieza.id, { x, y });
    }
  }

  protected terminarArrastre() {
    this.arrastre = null;
  }

  protected tocarFondo() {
    this.seleccion.set(null);
  }

  // ---------- Acciones sobre lo elegido ----------

  protected girarMesa(mesa: MesaEnPlano) {
    const medidas = this.medidas()!;
    const vertical = !mesa.vertical;
    const { ancho, alto } = tamanioMesa(medidas, vertical, mesa.mesitas);
    this.actualizarMesa(mesa.id, {
      vertical,
      x: limitar(mesa.x, 0, medidas.ancho - ancho),
      y: limitar(mesa.y, 0, medidas.alto - alto),
    });
  }

  protected girarElemento(e: ElementoEnPlano) {
    const medidas = this.medidas()!;
    const ancho = Math.min(e.alto, medidas.ancho);
    const alto = Math.min(e.ancho, medidas.alto);
    this.actualizarElemento(e.clave, {
      ancho,
      alto,
      x: limitar(e.x, 0, medidas.ancho - ancho),
      y: limitar(e.y, 0, medidas.alto - alto),
    });
  }

  protected moverMesaA(mesa: MesaEnPlano, sector: Sector) {
    this.actualizarMesa(mesa.id, { sectorId: sector.id });
    this.seleccion.set(null);
    this.avisos.ok(`La mesa ${mesa.etiqueta} pasó a ${sector.nombre}. Acordate de guardar.`);
  }

  protected borrarElemento(e: ElementoEnPlano) {
    this.elementos.update((lista) => lista.filter((x) => x.clave !== e.clave));
    this.seleccion.set(null);
    this.cambios.set(true);
  }

  protected async cambiarNumero(mesa: MesaEnPlano) {
    const texto = prompt(`Nuevo número para la mesa ${mesa.etiqueta}:`, String(mesa.numero));
    const numero = Number(texto);
    if (!texto || !Number.isInteger(numero) || numero < 1 || numero === mesa.numero) return;
    try {
      const cambiada = await this.api.cambiarNumeroMesa(mesa.id, numero);
      this.mesas.update((lista) =>
        lista.map((m) => (m.id === mesa.id ? { ...m, numero, etiqueta: cambiada.etiqueta } : m)),
      );
      this.avisos.ok(`Ahora es la mesa ${cambiada.etiqueta}`);
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  // ---------- Agregar cosas al plano ----------

  protected agregarElemento(tipo: TipoElemento) {
    const medidas = this.medidas()!;
    const sector = this.sectorActual();
    if (!sector) return;
    const { ancho, alto } = ELEMENTOS[tipo];
    const nuevo: ElementoEnPlano = {
      clave: this.siguienteClave++,
      sectorId: sector.id,
      tipo,
      ancho,
      alto,
      x: ajustar((medidas.ancho - ancho) / 2),
      y: ajustar((medidas.alto - alto) / 2),
    };
    this.elementos.update((lista) => [...lista, nuevo]);
    this.seleccion.set({ tipo: 'elemento', clave: nuevo.clave });
    this.cambios.set(true);
  }

  protected async agregarMesa() {
    const sector = this.sectorActual();
    if (!sector) return;
    const numero = Math.max(0, ...this.mesas().map((m) => m.numero)) + 1;
    try {
      const creada = await this.api.agregarMesa(numero, sector.id);
      this.mesas.update((lista) => [...lista, { ...creada, sectorId: sector.id }]);
      this.seleccion.set({ tipo: 'mesa', id: creada.id });
      this.avisos.ok(`Mesa ${numero} agregada. Arrastrala a su lugar.`);
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  protected async agregarSector() {
    const nombre = prompt('¿Cómo se llama el sector nuevo? (por ejemplo: Terraza)');
    if (!nombre?.trim()) return;
    try {
      const creado = await this.api.crearSector(nombre);
      this.sectores.update((lista) => [...lista, creado]);
      this.elegirSector(creado);
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  // ---------- Guardar ----------

  protected async guardar() {
    this.guardando.set(true);
    try {
      const salon = await this.api.guardarPlano({
        mesas: this.mesas().map(({ id, sectorId, x, y, vertical }) => ({ id, sectorId, x, y, vertical })),
        elementos: this.elementos().map(({ clave, ...elemento }) => elemento),
      });
      this.aplicarSalon(salon);
      this.cambios.set(false);
      this.avisos.ok('Plano guardado');
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.guardando.set(false);
    }
  }

  protected async descartar() {
    if (!confirm('¿Descartar lo que acomodaste y volver a como estaba?')) return;
    await this.cargar();
  }

  private async cargar() {
    try {
      const [salon, mesas] = await Promise.all([this.api.salon(), this.api.mesas()]);
      const primerSector = salon.sectores[0]?.id ?? 0;
      this.mesas.set(mesas.map((m) => ({ ...m, sectorId: m.sectorId ?? primerSector })));
      this.aplicarSalon(salon);
      this.seleccion.set(null);
      this.cambios.set(false);
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  private aplicarSalon(salon: Salon) {
    this.medidas.set(salon.medidas);
    this.sectores.set(salon.sectores);
    this.elementos.set(
      salon.sectores.flatMap((s) => s.elementos.map((e) => ({ ...e, sectorId: s.id, clave: this.siguienteClave++ }))),
    );
    this.seleccion.set(null);
  }

  private tamanio(pieza: MesaEnPlano | ElementoEnPlano) {
    return 'clave' in pieza ? { ancho: pieza.ancho, alto: pieza.alto } : tamanioMesa(this.medidas()!, pieza.vertical, pieza.mesitas);
  }

  private actualizarMesa(id: number, cambio: Partial<MesaEnPlano>) {
    this.mesas.update((lista) => lista.map((m) => (m.id === id ? { ...m, ...cambio } : m)));
    this.cambios.set(true);
  }

  private actualizarElemento(clave: number, cambio: Partial<ElementoEnPlano>) {
    this.elementos.update((lista) => lista.map((e) => (e.clave === clave ? { ...e, ...cambio } : e)));
    this.cambios.set(true);
  }
}

