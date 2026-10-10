package es.uni.reservas.security;

import es.uni.reservas.entity.Rol;

public record UsuarioAutenticado(Long id, String correo, Rol rol) {
}
