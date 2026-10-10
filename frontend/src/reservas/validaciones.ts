export interface DatosSolicitud {
  fecha: string
  horaInicio: string
  horaFin: string
}

const DURACION_MAXIMA_MINUTOS = 4 * 60

function aMinutos(hora: string): number {
  const [horas, minutos] = hora.split(':').map(Number)
  return horas * 60 + minutos
}

/**
 * Validación orientativa en el navegador. El backend es la fuente de verdad.
 * Devuelve el mensaje de error o null si los datos son válidos.
 */
export function validarSolicitud(datos: DatosSolicitud, ahora: Date): string | null {
  if (!datos.fecha || !datos.horaInicio || !datos.horaFin) {
    return 'Indique fecha, hora de inicio y hora de fin'
  }
  const inicio = aMinutos(datos.horaInicio)
  const fin = aMinutos(datos.horaFin)
  if (inicio % 30 !== 0 || fin % 30 !== 0) {
    return 'Las horas deben ser múltiplos de 30 minutos'
  }
  if (fin <= inicio) {
    return 'La hora de fin debe ser posterior a la de inicio'
  }
  if (fin - inicio > DURACION_MAXIMA_MINUTOS) {
    return 'Una reserva no puede durar más de cuatro horas consecutivas'
  }
  const comienzo = new Date(`${datos.fecha}T${datos.horaInicio}:00`)
  if (comienzo.getTime() < ahora.getTime()) {
    return 'No se pueden reservar fechas pasadas'
  }
  return null
}

export function aFechaHoraLocal(fecha: string, hora: string): string {
  return `${fecha}T${hora}:00`
}

export function horasDelDia(): string[] {
  const horas: string[] = []
  for (let minutos = 0; minutos < 24 * 60; minutos += 30) {
    const h = String(Math.floor(minutos / 60)).padStart(2, '0')
    const m = String(minutos % 60).padStart(2, '0')
    horas.push(`${h}:${m}`)
  }
  return horas
}
