import { useEffect, useState, type FormEvent } from 'react'
import { peticion } from '../api/cliente'
import type { EntradaHistorial, Espacio, EstadoReserva, Reserva } from '../api/tipos'
import { useAuth } from '../auth/AuthContext'
import { ETIQUETA_ESTADO, formatearFechaHora } from '../etiquetas'
import { aFechaHoraLocal, horasDelDia, validarSolicitud } from '../reservas/validaciones'

const HORAS = horasDelDia()
const ESTADOS_ACTIVOS: EstadoReserva[] = ['PENDIENTE', 'APROBADA']

export function ReservasPage() {
  const { sesion } = useAuth()
  const [espacios, setEspacios] = useState<Espacio[]>([])
  const [reservas, setReservas] = useState<Reserva[]>([])
  const [espacioId, setEspacioId] = useState('')
  const [fecha, setFecha] = useState('')
  const [horaInicio, setHoraInicio] = useState('08:00')
  const [horaFin, setHoraFin] = useState('10:00')
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [historialDe, setHistorialDe] = useState<number | null>(null)
  const [historial, setHistorial] = useState<EntradaHistorial[]>([])

  const cargar = async () => {
    const [listaEspacios, listaReservas] = await Promise.all([
      peticion<Espacio[]>('/api/espacios'),
      peticion<Reserva[]>('/api/reservas'),
    ])
    setEspacios(listaEspacios)
    setReservas(listaReservas)
  }

  useEffect(() => {
    cargar().catch((ex: Error) => setError(ex.message))
  }, [])

  const enviar = async (evento: FormEvent) => {
    evento.preventDefault()
    setError(null)
    setMensaje(null)
    const errorLocal = validarSolicitud({ fecha, horaInicio, horaFin }, new Date())
    if (errorLocal) {
      setError(errorLocal)
      return
    }
    if (!espacioId) {
      setError('Seleccione un espacio')
      return
    }
    try {
      await peticion('/api/reservas', {
        metodo: 'POST',
        cuerpo: {
          espacioId: Number(espacioId),
          inicio: aFechaHoraLocal(fecha, horaInicio),
          fin: aFechaHoraLocal(fecha, horaFin),
        },
      })
      setMensaje('Solicitud registrada. Queda pendiente de aprobación.')
      await cargar()
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'No se pudo crear la reserva')
    }
  }

  const cancelar = async (id: number) => {
    setError(null)
    setMensaje(null)
    try {
      await peticion(`/api/reservas/${id}/cancelar`, { metodo: 'POST' })
      setMensaje('Reserva cancelada.')
      await cargar()
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'No se pudo cancelar la reserva')
    }
  }

  const verHistorial = async (id: number) => {
    setError(null)
    try {
      setHistorial(await peticion<EntradaHistorial[]>(`/api/reservas/${id}/historial`))
      setHistorialDe(id)
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'No se pudo cargar el historial')
    }
  }

  return (
    <section>
      <h1>Reservas</h1>
      {error && <p role="alert" className="error">{error}</p>}
      {mensaje && <p role="status" className="aviso">{mensaje}</p>}

      <form className="tarjeta formulario" onSubmit={enviar}>
        <h2>Solicitar reserva</h2>
        <label>
          Espacio
          <select value={espacioId} onChange={(e) => setEspacioId(e.target.value)} required>
            <option value="">Seleccione un espacio</option>
            {espacios.map((espacio) => (
              <option key={espacio.id} value={espacio.id}>{espacio.nombre}</option>
            ))}
          </select>
        </label>
        <label>
          Fecha
          <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} required />
        </label>
        <label>
          Hora de inicio
          <select value={horaInicio} onChange={(e) => setHoraInicio(e.target.value)}>
            {HORAS.map((hora) => <option key={hora} value={hora}>{hora}</option>)}
          </select>
        </label>
        <label>
          Hora de fin
          <select value={horaFin} onChange={(e) => setHoraFin(e.target.value)}>
            {HORAS.map((hora) => <option key={hora} value={hora}>{hora}</option>)}
          </select>
        </label>
        <button type="submit">Solicitar</button>
      </form>

      <table className="tabla">
        <thead>
          <tr>
            <th>Espacio</th>
            <th>Inicio</th>
            <th>Fin</th>
            <th>Solicitante</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {reservas.map((reserva) => (
            <tr key={reserva.id}>
              <td>{reserva.espacioNombre}</td>
              <td>{formatearFechaHora(reserva.inicio)}</td>
              <td>{formatearFechaHora(reserva.fin)}</td>
              <td>{reserva.usuarioCorreo}</td>
              <td>
                {ETIQUETA_ESTADO[reserva.estado]}
                {reserva.motivoRechazo && <small> ({reserva.motivoRechazo})</small>}
              </td>
              <td className="acciones">
                {ESTADOS_ACTIVOS.includes(reserva.estado) && (
                  <button type="button" className="secundario" onClick={() => cancelar(reserva.id)}>
                    Cancelar
                  </button>
                )}
                <button type="button" className="secundario" onClick={() => verHistorial(reserva.id)}>
                  Historial
                </button>
              </td>
            </tr>
          ))}
          {reservas.length === 0 && (
            <tr>
              <td colSpan={6}>No hay reservas{sesion?.rol === 'SOLICITANTE' ? ' propias' : ''}.</td>
            </tr>
          )}
        </tbody>
      </table>

      {historialDe !== null && (
        <div className="tarjeta">
          <h2>Historial de la reserva {historialDe}</h2>
          <ul>
            {historial.map((entrada, indice) => (
              <li key={indice}>
                {formatearFechaHora(entrada.fecha)} · {entrada.estadoAnterior ? ETIQUETA_ESTADO[entrada.estadoAnterior] + ' → ' : ''}
                {ETIQUETA_ESTADO[entrada.estadoNuevo]} · {entrada.usuarioCorreo}
                {entrada.detalle && ` · ${entrada.detalle}`}
              </li>
            ))}
          </ul>
          <button type="button" className="secundario" onClick={() => setHistorialDe(null)}>Cerrar</button>
        </div>
      )}
    </section>
  )
}
