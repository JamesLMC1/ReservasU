import { useEffect, useState } from 'react'
import { peticion } from '../api/cliente'
import type { Reserva } from '../api/tipos'
import { useAuth } from '../auth/AuthContext'
import { formatearFechaHora } from '../etiquetas'

export function AprobacionesPage() {
  const { sesion } = useAuth()
  const [pendientes, setPendientes] = useState<Reserva[]>([])
  const [motivos, setMotivos] = useState<Record<number, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargar = async () => {
    setPendientes(await peticion<Reserva[]>('/api/reservas?estado=PENDIENTE'))
  }

  useEffect(() => {
    cargar().catch((ex: Error) => setError(ex.message))
  }, [])

  const ejecutar = async (accion: () => Promise<unknown>, exito: string, fallo: string) => {
    setError(null)
    setMensaje(null)
    try {
      await accion()
      setMensaje(exito)
      await cargar()
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : fallo)
    }
  }

  const aprobar = (id: number) =>
    ejecutar(() => peticion(`/api/reservas/${id}/aprobar`, { metodo: 'POST' }),
      'Solicitud aprobada.', 'No se pudo aprobar la solicitud')

  const rechazar = (id: number) =>
    ejecutar(() => peticion(`/api/reservas/${id}/rechazar`, { metodo: 'POST', cuerpo: { motivo: motivos[id] ?? '' } }),
      'Solicitud rechazada.', 'No se pudo rechazar la solicitud')

  return (
    <section>
      <h1>Solicitudes pendientes</h1>
      {error && <p role="alert" className="error">{error}</p>}
      {mensaje && <p role="status" className="aviso">{mensaje}</p>}

      <table className="tabla">
        <thead>
          <tr>
            <th>Espacio</th>
            <th>Inicio</th>
            <th>Fin</th>
            <th>Solicitante</th>
            <th>Decisión</th>
          </tr>
        </thead>
        <tbody>
          {pendientes.map((reserva) => {
            const propia = reserva.usuarioId === sesion?.id
            return (
              <tr key={reserva.id}>
                <td>{reserva.espacioNombre}</td>
                <td>{formatearFechaHora(reserva.inicio)}</td>
                <td>{formatearFechaHora(reserva.fin)}</td>
                <td>{reserva.usuarioCorreo}</td>
                <td className="acciones">
                  {propia ? (
                    <small>Solicitud propia: no puede decidirla</small>
                  ) : (
                    <>
                      <button type="button" onClick={() => aprobar(reserva.id)}>Aprobar</button>
                      <input
                        aria-label={`Motivo de rechazo ${reserva.id}`}
                        placeholder="Motivo de rechazo"
                        value={motivos[reserva.id] ?? ''}
                        onChange={(e) => setMotivos({ ...motivos, [reserva.id]: e.target.value })}
                      />
                      <button type="button" className="secundario" onClick={() => rechazar(reserva.id)}>
                        Rechazar
                      </button>
                    </>
                  )}
                </td>
              </tr>
            )
          })}
          {pendientes.length === 0 && (
            <tr>
              <td colSpan={5}>No hay solicitudes pendientes.</td>
            </tr>
          )}
        </tbody>
      </table>
    </section>
  )
}
