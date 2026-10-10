package es.uni.reservas.exception;

import org.springframework.http.HttpStatus;

public class DominioException extends RuntimeException {

    private final HttpStatus estado;

    public DominioException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
