import { createContext, useContext, useState, type ReactNode } from 'react'
import { peticion } from '../api/cliente'
import type { Sesion } from '../api/tipos'
import { borrarSesion, guardarSesion, leerSesion } from './sesion'

interface RespuestaLogin {
  token: string
  id: number
  nombre: string
  correo: string
  rol: Sesion['rol']
}

interface ValorAuth {
  sesion: Sesion | null
  iniciarSesion: (correo: string, contrasena: string) => Promise<void>
  cerrarSesion: () => void
}

const AuthContext = createContext<ValorAuth | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(() => leerSesion())

  const iniciarSesion = async (correo: string, contrasena: string) => {
    const respuesta = await peticion<RespuestaLogin>('/api/auth/login', {
      metodo: 'POST',
      cuerpo: { correo, contrasena },
    })
    const nueva: Sesion = {
      token: respuesta.token,
      id: respuesta.id,
      nombre: respuesta.nombre,
      correo: respuesta.correo,
      rol: respuesta.rol,
    }
    guardarSesion(nueva)
    setSesion(nueva)
  }

  const cerrarSesion = () => {
    borrarSesion()
    setSesion(null)
  }

  return (
    <AuthContext.Provider value={{ sesion, iniciarSesion, cerrarSesion }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): ValorAuth {
  const valor = useContext(AuthContext)
  if (!valor) {
    throw new Error('useAuth debe usarse dentro de AuthProvider')
  }
  return valor
}
