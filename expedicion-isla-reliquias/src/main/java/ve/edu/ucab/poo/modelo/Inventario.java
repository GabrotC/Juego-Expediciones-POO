package ve.edu.ucab.poo.modelo;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import java.util.ArrayList;
import java.util.List;

public class Inventario {
    private List<Reliquia> reliquias;

    public Inventario() {
        this.reliquias = new ArrayList<>();
    }

    public Inventario(List<Reliquia> iniciales) {
        this.reliquias = new ArrayList<>();
        if (iniciales != null) {
            this.reliquias.addAll(iniciales);
        }
    }

    public boolean estaLleno() {
        return reliquias.size() >= ReglasConstantes.CAPACIDAD_MAXIMA_INVENTARIO;
    }

    public boolean estaVacio() {
        return reliquias.isEmpty();
    }

    public void agregarReliquia(Reliquia reliquia) throws ReglaJuegoException {
        if (estaLleno()) {
            throw new ReglaJuegoException("Regrese al campamento para entregar antes de explorar.");
        }
        reliquias.add(reliquia);
    }

    public List<Reliquia> vaciarParaEntrega() throws ReglaJuegoException {
        if (estaVacio()) {
            throw new ReglaJuegoException("No lleva reliquias en el inventario para entregar.");
        }
        List<Reliquia> entregadas = new ArrayList<>(reliquias);
        reliquias.clear();
        return entregadas;
    }

    public List<Reliquia> getReliquias() {
        return reliquias;
    }
}