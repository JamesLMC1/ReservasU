import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { AuthProvider } from './AuthContext'
import { RutaProtegida } from './RutaProtegida'
import { guardarSesion } from './sesion'

function renderizar(ruta: string, roles?: Parameters<typeof RutaProtegida>[0]['roles']) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[ruta]}>
        <Routes>
          <Route path="/login" element={<p>Pantalla de acceso</p>} />
          <Route path="/" element={<p>Pantalla de inicio</p>} />
          <Route
            path="/privada"
            element={
              <RutaProtegida roles={roles}>
                <p>Contenido privado</p>
              </RutaProtegida>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('RutaProtegida', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => localStorage.clear())

  it('redirige al login cuando no hay sesión', () => {
    renderizar('/privada')

    expect(screen.getByText('Pantalla de acceso')).toBeInTheDocument()
    expect(screen.queryByText('Contenido privado')).not.toBeInTheDocument()
  })

  it('redirige al inicio cuando el rol no está permitido', () => {
    guardarSesion({ token: 't', id: 2, nombre: 'Ana', correo: 'ana@uni.es', rol: 'SOLICITANTE' })

    renderizar('/privada', ['APROBADOR', 'ADMINISTRADOR'])

    expect(screen.getByText('Pantalla de inicio')).toBeInTheDocument()
  })

  it('muestra el contenido cuando el rol está permitido', () => {
    guardarSesion({ token: 't', id: 3, nombre: 'Luis', correo: 'luis@uni.es', rol: 'APROBADOR' })

    renderizar('/privada', ['APROBADOR', 'ADMINISTRADOR'])

    expect(screen.getByText('Contenido privado')).toBeInTheDocument()
  })
})
