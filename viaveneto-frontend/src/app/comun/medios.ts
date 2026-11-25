import { MedioPago, TipoTurno } from './modelos';

export const MEDIOS: { medio: MedioPago; nombre: string; icono: string }[] = [
  { medio: 'EFECTIVO', nombre: 'Efectivo', icono: '💵' },
  { medio: 'DEBITO', nombre: 'Débito', icono: '💳' },
  { medio: 'CREDITO', nombre: 'Crédito', icono: '💳' },
  { medio: 'TRANSFERENCIA', nombre: 'Transferencia / QR', icono: '📱' },
];

export const nombreMedio = (medio: MedioPago) => MEDIOS.find((m) => m.medio === medio)?.nombre ?? medio;

export const nombreTurno = (tipo: TipoTurno) => (tipo === 'MEDIODIA' ? 'Mediodía' : 'Noche');

/** El descuento que suelen hacer cuando pagan todo en efectivo. */
export const DESCUENTO_EFECTIVO = 10;
