import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { Categoria, MesaResumen, Persona, Producto } from '../comun/modelos';
import { Sesion } from '../comun/sesion';

type ProductoEditable = Producto & { cambiado: boolean };

/** Pantalla para el dueño o la encargada: precios, productos nuevos y cantidad de mesas. */
@Component({
  selector: 'app-ajustes',
  imports: [FormsModule, RouterLink],
  styleUrl: './ajustes.css',
  templateUrl: './ajustes.html',
})
export class Ajustes {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  protected readonly sesion = inject(Sesion);
  protected readonly personas = signal<Persona[]>([]);
  protected nuevaPersona = { nombre: '', pin: '' };

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly productos = signal<ProductoEditable[]>([]);
  protected readonly mesas = signal<MesaResumen[]>([]);
  protected readonly guardando = signal(false);

  protected nuevo = { nombre: '', precio: null as number | null, categoriaId: null as number | null };
  protected nuevaCategoria = '';
  protected nuevaMesa: number | null = null;

  protected readonly productosPorCategoria = computed(() =>
    this.categorias().map((c) => ({ ...c, editables: this.productos().filter((p) => p.categoriaId === c.id) })),
  );
  protected readonly siguienteMesa = computed(() => Math.max(0, ...this.mesas().map((m) => m.numero)) + 1);

  constructor() {
    this.cargar();
  }

  protected marcarCambio(producto: ProductoEditable) {
    producto.cambiado = true;
  }

  protected async guardarProducto(producto: ProductoEditable) {
    await this.hacer(async () => {
      const { id, cambiado, ...form } = producto;
      await this.api.editarProducto(id, form);
      producto.cambiado = false;
      this.avisos.ok(`${producto.nombre} guardado`);
    });
  }

  protected async agregarProducto() {
    const { nombre, precio, categoriaId } = this.nuevo;
    if (!nombre.trim() || precio === null || categoriaId === null) {
      this.avisos.error('Completá nombre, precio y categoría.');
      return;
    }
    await this.hacer(async () => {
      await this.api.crearProducto({ nombre, precio, categoriaId, activo: true });
      this.avisos.ok(`${nombre.trim()} agregado a la carta`);
      this.nuevo = { nombre: '', precio: null, categoriaId };
      await this.cargar();
    });
  }

  protected async agregarCategoria() {
    if (!this.nuevaCategoria.trim()) return;
    await this.hacer(async () => {
      const creada = await this.api.crearCategoria(this.nuevaCategoria);
      this.avisos.ok(`Categoría ${creada.nombre} creada`);
      this.nuevaCategoria = '';
      this.nuevo.categoriaId = creada.id;
      await this.cargar();
    });
  }

  protected async agregarMesa() {
    const numero = this.nuevaMesa ?? this.siguienteMesa();
    await this.hacer(async () => {
      await this.api.agregarMesa(numero);
      this.avisos.ok(`Mesa ${numero} agregada`);
      this.nuevaMesa = null;
      this.mesas.set(await this.api.mesas());
    });
  }

  protected async quitarMesa(mesa: MesaResumen) {
    if (!confirm(`¿Quitar la mesa ${mesa.etiqueta} del salón?`)) return;
    await this.hacer(async () => {
      await this.api.quitarMesa(mesa.id);
      this.avisos.ok(`Mesa ${mesa.etiqueta} quitada`);
      this.mesas.set(await this.api.mesas());
    });
  }

  protected async agregarPersona() {
    const { nombre, pin } = this.nuevaPersona;
    if (!nombre.trim() || !/^\d{4}$/.test(pin)) {
      this.avisos.error('Poné el nombre y un PIN de 4 números.');
      return;
    }
    await this.hacer(async () => {
      await this.api.crearUsuario(nombre, pin);
      this.avisos.ok(`${nombre.trim()} ya puede entrar con su PIN`);
      this.nuevaPersona = { nombre: '', pin: '' };
      this.personas.set(await this.api.usuarios());
    });
  }

  protected async cambiarPin(persona: Persona) {
    const pin = prompt(`Nuevo PIN de 4 números para ${persona.nombre}:`);
    if (pin === null) return;
    if (!/^\d{4}$/.test(pin)) {
      this.avisos.error('El PIN tiene que ser de 4 números.');
      return;
    }
    await this.hacer(async () => {
      await this.api.cambiarPin(persona.id, pin);
      this.avisos.ok(`PIN de ${persona.nombre} cambiado`);
    });
  }

  protected async quitarPersona(persona: Persona) {
    if (!confirm(`¿Quitar a ${persona.nombre}? Ya no va a poder entrar.`)) return;
    await this.hacer(async () => {
      await this.api.quitarUsuario(persona.id);
      this.avisos.ok(`${persona.nombre} ya no puede entrar`);
      this.personas.set(await this.api.usuarios());
    });
  }

  private async cargar() {
    try {
      const [carta, mesas, personas] = await Promise.all([this.api.carta(false), this.api.mesas(), this.api.usuarios()]);
      this.personas.set(personas);
      this.categorias.set(carta);
      this.productos.set(carta.flatMap((c) => c.productos.map((p) => ({ ...p, cambiado: false }))));
      this.mesas.set(mesas);
      this.nuevo.categoriaId ??= carta[0]?.id ?? null;
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  private async hacer(accion: () => Promise<void>) {
    this.guardando.set(true);
    try {
      await accion();
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.guardando.set(false);
    }
  }
}
