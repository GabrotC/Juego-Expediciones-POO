package ve.edu.ucab.poo.modelo;

import java.util.Objects;

public class Reliquia {
    private String reliquiaId;
    private int valor;

    public Reliquia(String reliquiaId, int valor) {
        this.reliquiaId = reliquiaId;
        this.valor = valor;
    }

    public String getReliquiaId() {
        return reliquiaId;
    }

    public int getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reliquia reliquia = (Reliquia) o;
        return Objects.equals(reliquiaId, reliquia.reliquiaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reliquiaId);
    }

    @Override
    public String toString() {
        return reliquiaId + " (valor " + valor + ")";
    }
}