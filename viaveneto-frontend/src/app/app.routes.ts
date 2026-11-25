import { Routes } from '@angular/router';
import { conSesion } from './comun/con-sesion';

/** Todo lo que se ve después de poner el PIN. */
const conPin: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'mesas' },
  { path: 'mesas', title: 'Mesas · Via Veneto', loadComponent: () => import('./mesas/mesas').then((m) => m.Mesas) },
  {
    path: 'pedidos/:id/cobrar',
    title: 'Cobrar · Via Veneto',
    loadComponent: () => import('./cobro/cobro').then((m) => m.Cobro),
  },
  {
    path: 'pedidos/:id/ticket',
    title: 'Ticket · Via Veneto',
    loadComponent: () => import('./ticket/ticket').then((m) => m.TicketPedido),
  },
  { path: 'caja', title: 'Caja · Via Veneto', loadComponent: () => import('./caja/caja').then((m) => m.CajaPagina) },
  {
    path: 'caja/turnos/:id/ticket',
    title: 'Cierre de caja · Via Veneto',
    loadComponent: () => import('./ticket/cierre').then((m) => m.TicketCierre),
  },
  {
    path: 'pedidos/:id',
    title: 'Pedido · Via Veneto',
    loadComponent: () => import('./pedido/pedido').then((m) => m.PedidoPagina),
  },
  { path: 'stock', title: 'Stock · Via Veneto', loadComponent: () => import('./stock/stock').then((m) => m.StockPagina) },
  { path: 'carta', title: 'Carta y mesas · Via Veneto', loadComponent: () => import('./ajustes/ajustes').then((m) => m.Ajustes) },
  {
    path: 'salon',
    title: 'Acomodar salón · Via Veneto',
    loadComponent: () => import('./salon/acomodar').then((m) => m.AcomodarSalon),
    canDeactivate: [
      (pagina: { cambios: () => boolean }) =>
        !pagina.cambios() || confirm('Tenés cambios sin guardar en el plano. ¿Salir igual?'),
    ],
  },
  { path: '**', redirectTo: 'mesas' },
];

export const routes: Routes = [
  {
    path: 'ingresar',
    title: 'Ingresar · Via Veneto',
    loadComponent: () => import('./ingreso/ingreso').then((m) => m.Ingreso),
  },
  { path: '', canActivateChild: [conSesion], children: conPin },
];
