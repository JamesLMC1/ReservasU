import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { guardarSesion } from '../auth/sesion'
import type { EntradaHistorial, Reserva } from '../api/tipos'
import { ReservasPage } from './ReservasPage'

function json(datos: unknown) {
  return { ok: true, status: 200, text: async () => JSON.stringify(datos) }
}

function errorJson(status: number, mensaje: string) {
  return { ok: false, status, text: async () => JSON.stringify({ mensaje }) }
}

function reserva(parcial: Partial<Reserva>): Reserva {
  return {
    id: 1,
    espacioId: 1,
    espacioNombre: 'Lab 1',
    usuarioId: 2,
    usuarioCorreo: 'ana@uni.es',
    inicio: '2030-01-10T10:00:00+01:00',
    fin: '2030-01-10T12:00:00+01:00',
    estado: 'PENDIENTE',
    motivoRechazo: null,
    ...parcial,
  }
}

function renderizar() {
  return render(
    <MemoryRouter initialEntries={['/reservas']}>
      <AuthProvider>
        <ReservasPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

function rellenar(fecha: string, inicio: string, fin: string) {
  fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: fecha } })
  fireEvent.change(screen.getByLabelText('Hora de inicio'), { target: { value: inicio } })
  fireEvent.change(screen.getByLabelText('Hora de fin'), { target: { value: fin } })
}

describe('ReservasPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    localStorage.clear()
    guardarSesion({ token: 't', id: 2, nombre: 'Ana', correo: 'ana@uni.es', rol: 'SOLICITANTE' })
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('rechaza una reserva de mas de cuatro horas antes de enviar', async () => {
    fetchMock
      .mockResolvedValueOnce(json([{ id: 1, nombre: 'Lab 1', tipo: 'LABORATORIO', activo: true }]))
      .mockResolvedValueOnce(json([]))
    renderizar()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))

    fireEvent.change(screen.getByLabelText('Espacio'), { target: { value: '1' } })
    rellenar('2030-01-10', '10:00', '14:30')
    fireEvent.click(screen.getByRole('button', { name: 'Solicitar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/cuatro horas/)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('rechaza horas no alineadas a 30 minutos antes de enviar', async () => {
    fetchMock
      .mockResolvedValueOnce(json([{ id: 1, nombre: 'Lab 1', tipo: 'LABORATORIO', activo: true }]))
      .mockResolvedValueOnce(json([]))
    renderizar()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))

    fireEvent.change(screen.getByLabelText('Espacio'), { target: { value: '1' } })
    rellenar('2030-01-10', '10:15', '12:00')
    fireEvent.click(screen.getByRole('button', { name: 'Solicitar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/30 minutos/)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('muestra Cancelar solo en reservas activas y siempre Historial', async () => {
    fetchMock
      .mockResolvedValueOnce(json([]))
      .mockResolvedValueOnce(json([
        reserva({ id: 1, estado: 'PENDIENTE' }),
        reserva({ id: 2, estado: 'APROBADA' }),
        reserva({ id: 3, estado: 'CANCELADA' }),
      ]))
    renderizar()

    await waitFor(() => expect(screen.getAllByRole('button', { name: 'Historial' })).toHaveLength(3))
    expect(screen.getAllByRole('button', { name: 'Cancelar' })).toHaveLength(2)
  })

  it('carga el historial de una reserva al pulsar Historial', async () => {
    const historial: EntradaHistorial[] = [
      {
        estadoAnterior: null,
        estadoNuevo: 'PENDIENTE',
        usuarioCorreo: 'ana@uni.es',
        fecha: '2030-01-01T09:00:00+01:00',
        detalle: 'Solicitud creada',
      },
    ]
    fetchMock
      .mockResolvedValueOnce(json([]))
      .mockResolvedValueOnce(json([reserva({ id: 1 })]))
      .mockResolvedValueOnce(json(historial))
    renderizar()

    await screen.findByText('Lab 1')
    fireEvent.click(screen.getAllByRole('button', { name: 'Historial' })[0])

    expect(await screen.findByText(/Historial de la reserva 1/)).toBeInTheDocument()
    expect(screen.getByText(/Solicitud creada/)).toBeInTheDocument()
  })

  it('muestra el mensaje del backend cuando rechaza la solicitud', async () => {
    fetchMock
      .mockResolvedValueOnce(json([{ id: 1, nombre: 'Lab 1', tipo: 'LABORATORIO', activo: true }]))
      .mockResolvedValueOnce(json([]))
      .mockResolvedValueOnce(errorJson(409, 'El espacio ya está reservado en ese horario'))
    renderizar()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))

    fireEvent.change(screen.getByLabelText('Espacio'), { target: { value: '1' } })
    rellenar('2030-01-10', '10:00', '12:00')
    fireEvent.click(screen.getByRole('button', { name: 'Solicitar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/ya está reservado/)
  })
})
