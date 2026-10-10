import { describe, expect, it } from 'vitest'
import { validarSolicitud } from './validaciones'

const ahora = new Date('2026-10-09T10:00:00')

describe('validarSolicitud', () => {
  it('acepta una reserva válida de cuatro horas exactas', () => {
    const resultado = validarSolicitud(
      { fecha: '2026-10-12', horaInicio: '10:00', horaFin: '14:00' },
      ahora,
    )
    expect(resultado).toBeNull()
  })

  it('rechaza una reserva de más de cuatro horas', () => {
    const resultado = validarSolicitud(
      { fecha: '2026-10-12', horaInicio: '10:00', horaFin: '14:30' },
      ahora,
    )
    expect(resultado).toMatch(/cuatro horas/)
  })

  it('rechaza un fin que no es posterior al inicio', () => {
    const resultado = validarSolicitud(
      { fecha: '2026-10-12', horaInicio: '10:00', horaFin: '10:00' },
      ahora,
    )
    expect(resultado).toMatch(/posterior/)
  })

  it('rechaza horas que no son múltiplos de 30 minutos', () => {
    const resultado = validarSolicitud(
      { fecha: '2026-10-12', horaInicio: '10:15', horaFin: '12:00' },
      ahora,
    )
    expect(resultado).toMatch(/30 minutos/)
  })

  it('rechaza fechas pasadas', () => {
    const resultado = validarSolicitud(
      { fecha: '2026-10-08', horaInicio: '10:00', horaFin: '11:00' },
      ahora,
    )
    expect(resultado).toMatch(/pasadas/)
  })

  it('exige fecha y horas', () => {
    const resultado = validarSolicitud({ fecha: '', horaInicio: '10:00', horaFin: '11:00' }, ahora)
    expect(resultado).toMatch(/Indique/)
  })
})
