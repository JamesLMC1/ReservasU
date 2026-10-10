import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { guardarSesion } from '../auth/sesion'
import { Layout } from './Layout'

function renderizarConRol(rol: 'ADMINISTRADOR' | 'APROBADOR' | 'SOLICITANTE') {
  guardarSesion({ token: 't', id: 1, nombre: 'Prueba', correo: 'prueba@uni.es', rol })
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/reservas']}>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/reservas" element={<p>Contenido</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('Layout', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => localStorage.clear())

  it('muestra Aprobaciones y Usuarios solo al administrador', () => {
    renderizarConRol('ADMINISTRADOR')

    expect(screen.getByRole('link', { name: 'Aprobaciones' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Usuarios' })).toBeInTheDocument()
  })

  it('muestra Aprobaciones pero no Usuarios al aprobador', () => {
    renderizarConRol('APROBADOR')

    expect(screen.getByRole('link', { name: 'Aprobaciones' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument()
  })

  it('oculta Aprobaciones y Usuarios al solicitante', () => {
    renderizarConRol('SOLICITANTE')

    expect(screen.queryByRole('link', { name: 'Aprobaciones' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Reservas' })).toBeInTheDocument()
  })
})
