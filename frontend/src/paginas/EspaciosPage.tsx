import { useEffect, useState, type FormEvent } from 'react'
import { peticion } from '../api/cliente'
import type { Espacio, TipoEspacio } from '../api/tipos'
import { useAuth } from '../auth/AuthContext'
import { ETIQUETA_TIPO } from '../etiquetas'

const TIPOS: TipoEspacio[] = ['LABORATORIO', 'AUDITORIO', 'SALA_COMPUTO']

export function EspaciosPage() {
  const { sesion } = useAuth()
  const esAdministrador = sesion?.rol === 'ADMINISTRADOR'
  const [espacios, setEspacios] = useState<Espacio[]>([])
  const [nombre, setNombre] = useState('')
  const [tipo, setTipo] = useState<TipoEspacio>('LABORATORIO')
  const [error, setError] = useState<string | null>(null)

  const cargar = async () => {
    setEspacios(await peticion<Espacio[]>('/api/espacios'))
  }

  useEffect(() => {
    cargar().catch((ex: Error) => setError(ex.message))
  }, [])

  const crear = async (evento: FormEvent) => {
    evento.preventDefault()
    setError(null)
    try {
      await peticion('/api/espacios', { metodo: 'POST', cuerpo: { nombre, tipo } })
      setNombre('')
      await cargar()
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'No se pudo crear el espacio')
    }
  }

  const desactivar = async (id: number) => {
    setError(null)
    try {
      await peticion(`/api/espacios/${id}/desactivar`, { metodo: 'PATCH' })
      await cargar()
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'No se pudo desactivar el espacio')
    }
  }

  return (
    <section>
      <h1>Espacios académicos</h1>
      {error && <p role="alert" className="error">{error}</p>}

      {esAdministrador && (
        <form className="tarjeta formulario" onSubmit={crear}>
          <h2>Nuevo espacio</h2>
          <label>
            Nombre
            <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={120} />
          </label>
          <label>
            Tipo
            <select value={tipo} onChange={(e) => setTipo(e.target.value as TipoEspacio)}>
              {TIPOS.map((t) => (
                <option key={t} value={t}>{ETIQUETA_TIPO[t]}</option>
              ))}
            </select>
          </label>
          <button type="submit">Crear espacio</button>
        </form>
      )}

      <table className="tabla">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Tipo</th>
            {esAdministrador && <th></th>}
          </tr>
        </thead>
        <tbody>
          {espacios.map((espacio) => (
            <tr key={espacio.id}>
              <td>{espacio.nombre}</td>
              <td>{ETIQUETA_TIPO[espacio.tipo]}</td>
              {esAdministrador && (
                <td>
                  <button type="button" className="secundario" onClick={() => desactivar(espacio.id)}>
                    Desactivar
                  </button>
                </td>
              )}
            </tr>
          ))}
          {espacios.length === 0 && (
            <tr>
              <td colSpan={esAdministrador ? 3 : 2}>No hay espacios activos.</td>
            </tr>
          )}
        </tbody>
      </table>
    </section>
  )
}
