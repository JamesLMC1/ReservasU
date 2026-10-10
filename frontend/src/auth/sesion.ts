import type { Sesion } from '../api/tipos'

const CLAVE = 'reservas.sesion'

export function leerSesion(): Sesion | null {
  const guardado = localStorage.getItem(CLAVE)
  if (!guardado) {
    return null
  }
  try {
    return JSON.parse(guardado) as Sesion
  } catch {
    localStorage.removeItem(CLAVE)
    return null
  }
}

export function guardarSesion(sesion: Sesion): void {
  localStorage.setItem(CLAVE, JSON.stringify(sesion))
}

export function borrarSesion(): void {
  localStorage.removeItem(CLAVE)
}
