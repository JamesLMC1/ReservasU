import { useEffect, useState, type FormEvent } from 'react'
import { peticion } from '../api/cliente'
import type { Rol, Usuario } from '../api/tipos'
import { useAuth } from '../auth/AuthContext'
import { ETIQUETA_ROL } from '../etiquetas'

const ROLES: Rol[] = ['ADMINISTRADOR', 'APROBADOR', 'SOLICITANTE']

export function UsuariosPage() {
  const { sesion } = useAuth()
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [nombre, setNombre] = useState('')
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [rol, setRol] = useState<Rol>('SOLICITANTE')
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const cargar = async () => {
    setUsuarios(await peticion<Usuario[]>('/api/usuarios'))
  }

  useEffect(() => {
    cargar().catch((ex: Error) => setError(ex.message))
  }, [])

  const ejecutar = async (accion: () => Promise<unknown>, exito: string) => {
    setError(null)
    setMensaje(null)
    try {
      await accion()
      setMensaje(exito)
      await cargar()
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'Ha ocurrido un error')
    }
  }

  const crear = async (evento: FormEvent) => {
    evento.preventDefault()
    await ejecutar(async () => {
      await peticion('/api/usuarios', { metodo: 'POST', cuerpo: { nombre, correo, contrasena, rol } })
      setNombre('')
      setCorreo('')
      setContrasena('')
    }, 'Usuario creado.')
  }

  const cambiarRol = (id: number, nuevoRol: Rol) =>
    ejecutar(() => peticion(`/api/usuarios/${id}/rol`, { metodo: 'PATCH', cuerpo: { rol: nuevoRol } }),
      'Rol actualizado.')

  const desactivar = (id: number) =>
    ejecutar(() => peticion(`/api/usuarios/${id}/desactivar`, { metodo: 'PATCH' }), 'Usuario desactivado.')

  return (
    <section>
      <h1>Usuarios</h1>
      {error && <p role="alert" className="error">{error}</p>}
      {mensaje && <p role="status" className="aviso">{mensaje}</p>}

      <form className="tarjeta formulario" onSubmit={crear}>
        <h2>Nuevo usuario</h2>
        <label>
          Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={120} />
        </label>
        <label>
          Correo
          <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} required />
        </label>
        <label>
          Contraseña (mínimo 8 caracteres)
          <input type="password" value={contrasena} onChange={(e) => setContrasena(e.target.value)}
                 required minLength={8} />
        </label>
        <label>
          Rol
          <select value={rol} onChange={(e) => setRol(e.target.value as Rol)}>
            {ROLES.map((r) => <option key={r} value={r}>{ETIQUETA_ROL[r]}</option>)}
          </select>
        </label>
        <button type="submit">Crear usuario</button>
      </form>

      <table className="tabla">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Correo</th>
            <th>Rol</th>
            <th>Estado</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((usuario) => (
            <tr key={usuario.id}>
              <td>{usuario.nombre}</td>
              <td>{usuario.correo}</td>
              <td>
                <select value={usuario.rol} disabled={!usuario.activo}
                        aria-label={`Rol de ${usuario.correo}`}
                        onChange={(e) => cambiarRol(usuario.id, e.target.value as Rol)}>
                  {ROLES.map((r) => <option key={r} value={r}>{ETIQUETA_ROL[r]}</option>)}
                </select>
              </td>
              <td>{usuario.activo ? 'Activo' : 'Desactivado'}</td>
              <td>
                {usuario.activo && usuario.id !== sesion?.id && (
                  <button type="button" className="secundario" onClick={() => desactivar(usuario.id)}>
                    Desactivar
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
