export type Rol = 'ADMINISTRADOR' | 'APROBADOR' | 'SOLICITANTE'
export type TipoEspacio = 'LABORATORIO' | 'AUDITORIO' | 'SALA_COMPUTO'
export type EstadoReserva = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA'

export interface Sesion {
  token: string
  id: number
  nombre: string
  correo: string
  rol: Rol
}

export interface Usuario {
  id: number
  nombre: string
  correo: string
  rol: Rol
  activo: boolean
}

export interface Espacio {
  id: number
  nombre: string
  tipo: TipoEspacio
  activo: boolean
}

export interface Reserva {
  id: number
  espacioId: number
  espacioNombre: string
  usuarioId: number
  usuarioCorreo: string
  inicio: string
  fin: string
  estado: EstadoReserva
  motivoRechazo: string | null
}

export interface EntradaHistorial {
  estadoAnterior: EstadoReserva | null
  estadoNuevo: EstadoReserva
  usuarioCorreo: string
  fecha: string
  detalle: string | null
}
