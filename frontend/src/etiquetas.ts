import type { EstadoReserva, Rol, TipoEspacio } from './api/tipos'

export const ETIQUETA_ROL: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  APROBADOR: 'Aprobador',
  SOLICITANTE: 'Solicitante',
}

export const ETIQUETA_TIPO: Record<TipoEspacio, string> = {
  LABORATORIO: 'Laboratorio',
  AUDITORIO: 'Auditorio',
  SALA_COMPUTO: 'Sala de cómputo',
}

export const ETIQUETA_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  CANCELADA: 'Cancelada',
}

export function formatearFechaHora(valor: string): string {
  return new Date(valor).toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'short' })
}
