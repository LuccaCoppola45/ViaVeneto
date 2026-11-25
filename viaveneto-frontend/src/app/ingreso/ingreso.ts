import { Component, HostListener, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api } from '../comun/api';
import { Persona, SesionNueva } from '../comun/modelos';
import { Sesion } from '../comun/sesion';

const LARGO_PIN = 4;
const TECLAS = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];

/**
 * Pantalla para entrar: se toca el nombre y se pone el PIN en un teclado grande.
 * La primera vez que se usa el sistema, pide crear a la primera persona.
 */
@Component({
  selector: 'app-ingreso',
  styleUrl: './ingreso.css',
  templateUrl: './ingreso.html',
})
export class Ingreso implements OnInit {
  /** A dónde volver después de ingresar (viene en la URL). */
  readonly volver = input<string>();

  private readonly api = inject(Api);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly TECLAS = TECLAS;
  protected readonly LARGO_PIN = LARGO_PIN;

  protected readonly cargando = signal(true);
  protected readonly hayUsuarios = signal(true);
  protected readonly personas = signal<Persona[]>([]);
  protected readonly elegida = signal<Persona | null>(null);
  protected readonly pin = signal('');
  protected readonly error = signal<string | null>(null);
  protected readonly trabajando = signal(false);

  // Primera vez
  protected readonly nombreNuevo = signal('');
  protected readonly pinRepetido = signal('');
  protected readonly paso = signal<'nombre' | 'pin' | 'repetir'>('nombre');

  protected readonly puntos = computed(() => Array.from({ length: LARGO_PIN }, (_, i) => i < this.pin().length));

  async ngOnInit() {
    try {
      const estado = await this.api.estadoIngreso();
      this.hayUsuarios.set(estado.hayUsuarios);
      this.personas.set(estado.usuarios);
      if (estado.usuarios.length === 1) this.elegida.set(estado.usuarios[0]);
    } catch (error) {
      this.error.set((error as Error).message);
    } finally {
      this.cargando.set(false);
    }
  }

  protected elegir(persona: Persona) {
    this.elegida.set(persona);
    this.pin.set('');
    this.error.set(null);
  }

  protected otraPersona() {
    this.elegida.set(null);
    this.pin.set('');
    this.error.set(null);
  }

  protected tocar(digito: string) {
    if (this.trabajando() || this.pin().length >= LARGO_PIN) return;
    this.error.set(null);
    this.pin.update((p) => p + digito);
    if (this.pin().length === LARGO_PIN) this.completo();
  }

  protected borrar() {
    this.pin.update((p) => p.slice(0, -1));
  }

  /** También se puede escribir con el teclado de la compu de la caja. */
  @HostListener('document:keydown', ['$event'])
  protected teclado(evento: KeyboardEvent) {
    const escribiendoNombre = !this.hayUsuarios() && this.paso() === 'nombre';
    if (escribiendoNombre || (this.hayUsuarios() && !this.elegida())) return;
    if (/^\d$/.test(evento.key)) this.tocar(evento.key);
    if (evento.key === 'Backspace') this.borrar();
  }

  protected seguirConElPin() {
    if (!this.nombreNuevo().trim()) {
      this.error.set('Poné tu nombre.');
      return;
    }
    this.error.set(null);
    this.paso.set('pin');
  }

  private async completo() {
    if (this.hayUsuarios()) {
      await this.ingresar();
    } else if (this.paso() === 'pin') {
      this.pinRepetido.set(this.pin());
      this.pin.set('');
      this.paso.set('repetir');
    } else {
      await this.crearPrimera();
    }
  }

  private async ingresar() {
    const persona = this.elegida();
    if (!persona) return;
    await this.intentar(() => this.api.ingresar(persona.id, this.pin()));
  }

  private async crearPrimera() {
    if (this.pin() !== this.pinRepetido()) {
      this.error.set('Los dos PIN no son iguales. Empezá de nuevo.');
      this.pin.set('');
      this.paso.set('pin');
      return;
    }
    await this.intentar(() => this.api.crearPrimerUsuario(this.nombreNuevo(), this.pin()));
  }

  private async intentar(llamada: () => Promise<SesionNueva>) {
    this.trabajando.set(true);
    try {
      const nueva = await llamada();
      this.sesion.iniciar(nueva.token, nueva.usuario);
      this.router.navigateByUrl(this.volver() || '/mesas');
    } catch (error) {
      this.error.set((error as Error).message);
      this.pin.set('');
    } finally {
      this.trabajando.set(false);
    }
  }
}
