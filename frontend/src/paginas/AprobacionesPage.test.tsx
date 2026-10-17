import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { guardarSesion } from '../auth/sesion'
import type { Reserva } from '../api/tipos'
import { AprobacionesPage } from './AprobacionesPage'

function json(datos: unknown) {
  return { ok: true, status: 200, text: async () => JSON.stringify(datos) }
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

describe('AprobacionesPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    localStorage.clear()
    guardarSesion({ token: 't', id: 3, nombre: 'Luis', correo: 'luis@uni.es', rol: 'APROBADOR' })
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('no permite decidir sobre la solicitud propia y si sobre las ajenas', async () => {
    fetchMock.mockResolvedValueOnce(json([
      reserva({ id: 1, usuarioId: 3, usuarioCorreo: 'luis@uni.es' }),
      reserva({ id: 2, usuarioId: 2, usuarioCorreo: 'ana@uni.es' }),
    ]))

    render(
      <MemoryRouter>
        <AuthProvider>
          <AprobacionesPage />
        </AuthProvider>
      </MemoryRouter>,
    )

    expect(await screen.findByText(/no puede decidirla/)).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Aprobar' })).toHaveLength(1)
    expect(screen.getAllByRole('button', { name: 'Rechazar' })).toHaveLength(1)
  })
})
