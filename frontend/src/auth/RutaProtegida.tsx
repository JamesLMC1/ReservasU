import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import type { Rol } from '../api/tipos'
import { useAuth } from './AuthContext'

interface Props {
  roles?: Rol[]
  children: ReactNode
}

export function RutaProtegida({ roles, children }: Props) {
  const { sesion } = useAuth()
  if (!sesion) {
    return <Navigate to="/login" replace />
  }
  if (roles && !roles.includes(sesion.rol)) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
