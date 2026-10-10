import { Navigate, Route, Routes } from 'react-router-dom'
import { RutaProtegida } from './auth/RutaProtegida'
import { Layout } from './componentes/Layout'
import { AprobacionesPage } from './paginas/AprobacionesPage'
import { EspaciosPage } from './paginas/EspaciosPage'
import { LoginPage } from './paginas/LoginPage'
import { ReservasPage } from './paginas/ReservasPage'
import { UsuariosPage } from './paginas/UsuariosPage'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RutaProtegida>
            <Layout />
          </RutaProtegida>
        }
      >
        <Route path="/" element={<Navigate to="/reservas" replace />} />
        <Route path="/reservas" element={<ReservasPage />} />
        <Route path="/espacios" element={<EspaciosPage />} />
        <Route
          path="/aprobaciones"
          element={
            <RutaProtegida roles={['APROBADOR', 'ADMINISTRADOR']}>
              <AprobacionesPage />
            </RutaProtegida>
          }
        />
        <Route
          path="/usuarios"
          element={
            <RutaProtegida roles={['ADMINISTRADOR']}>
              <UsuariosPage />
            </RutaProtegida>
          }
        />
      </Route>
    </Routes>
  )
}
