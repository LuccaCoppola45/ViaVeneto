/**
 * Abre el ticket en una ventana aparte y lo manda a imprimir.
 * Así la pantalla donde se está trabajando queda como estaba.
 */
export const abrirTicket = (pedidoId: number) => abrir(`/pedidos/${pedidoId}/ticket?imprimir=1`);

export const abrirCierreDeTurno = (turnoId: number) => abrir(`/caja/turnos/${turnoId}/ticket?imprimir=1`);

const abrir = (url: string) => {
  const ventana = window.open(url, '_blank', 'width=420,height=720');
  if (!ventana) {
    // Si el navegador bloquea ventanas nuevas, se abre en la misma.
    window.location.href = url;
  }
};
