import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ETIQUETA_ROL } from '../etiquetas'

export function Layout() {
  const { sesion, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const puedeAprobar = sesion?.rol === 'APROBADOR' || sesion?.rol === 'ADMINISTRADOR'
  const esAdministrador = sesion?.rol === 'ADMINISTRADOR'

  const salir = () => {
    cerrarSesion()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app">
      <header className="cabecera">
        <span className="marca">Reservas de Espacios Académicos</span>
        <nav className="menu">
          <NavLink to="/reservas">Reservas</NavLink>
          <NavLink to="/espacios">Espacios</NavLink>
          {puedeAprobar && <NavLink to="/aprobaciones">Aprobaciones</NavLink>}
          {esAdministrador && <NavLink to="/usuarios">Usuarios</NavLink>}
        </nav>
        <div className="sesion">
          <span>
            {sesion?.nombre} · {sesion && ETIQUETA_ROL[sesion.rol]}
          </span>
          <button type="button" className="secundario" onClick={salir}>
            Cerrar sesión
          </button>
        </div>
      </header>
      <main className="contenido">
        <Outlet />
      </main>
    </div>
  )
}
