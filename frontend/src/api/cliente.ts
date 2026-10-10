import { leerSesion } from '../auth/sesion'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ErrorApi extends Error {
  readonly estado: number

  constructor(estado: number, mensaje: string) {
    super(mensaje)
    this.estado = estado
  }
}

interface Opciones {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH'
  cuerpo?: unknown
}

export async function peticion<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  const cabeceras: Record<string, string> = {}
  if (opciones.cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json'
  }
  const sesion = leerSesion()
  if (sesion) {
    cabeceras['Authorization'] = `Bearer ${sesion.token}`
  }

  const respuesta = await fetch(`${BASE_URL}${ruta}`, {
    method: opciones.metodo ?? 'GET',
    headers: cabeceras,
    body: opciones.cuerpo !== undefined ? JSON.stringify(opciones.cuerpo) : undefined,
  })

  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : undefined
  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, datos?.mensaje ?? 'Ha ocurrido un error inesperado')
  }
  return datos as T
}
