import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { guardarSesion } from '../auth/sesion'
import type { Rol } from '../api/tipos'
import { EspaciosPage } from './EspaciosPage'

function json(datos: unknown) {
  return { ok: true, status: 200, text: async () => JSON.stringify(datos) }
}

function renderizar(rol: Rol) {
  guardarSesion({ token: 't', id: 1, nombre: 'Prueba', correo: 'prueba@uni.es', rol })
  return render(
    <MemoryRouter>
      <AuthProvider>
        <EspaciosPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('EspaciosPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    localStorage.clear()
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('el administrador ve el formulario de alta', async () => {
    fetchMock.mockResolvedValueOnce(json([]))
    renderizar('ADMINISTRADOR')

    expect(await screen.findByText('Nuevo espacio')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Crear espacio' })).toBeInTheDocument()
  })

  it('el solicitante no ve el formulario de alta', async () => {
    fetchMock.mockResolvedValueOnce(json([]))
    renderizar('SOLICITANTE')

    await waitFor(() => expect(screen.getByText('No hay espacios activos.')).toBeInTheDocument())
    expect(screen.queryByText('Nuevo espacio')).not.toBeInTheDocument()
  })
})
