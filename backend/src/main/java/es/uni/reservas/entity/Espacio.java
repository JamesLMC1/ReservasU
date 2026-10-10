package es.uni.reservas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "espacio")
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEspacio tipo;

    @Column(nullable = false)
    private boolean activo = true;

    protected Espacio() {
    }

    public Espacio(String nombre, TipoEspacio tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public void modificar(String nombre, TipoEspacio tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoEspacio getTipo() {
        return tipo;
    }

    public boolean isActivo() {
        return activo;
    }
}
