package ve.edu.ucab.poo.modelo;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import java.util.ArrayList;
import java.util.List;

public class Participante {
    private Jugador jugador;
    private String posicion;
    private int puntos;
    private Inventario inventario;
    private List<Reliquia> entregadas;
    private long tiempoConsumidoSegundos;

    public Participante(Jugador jugador, String posicionInicial) {
        this.jugador = jugador;
        this.posicion = posicionInicial.toUpperCase();
        this.puntos = 0;
        this.inventario = new Inventario();
        this.entregadas = new ArrayList<>();
        this.tiempoConsumidoSegundos = 0;
    }

    public Participante(Jugador jugador, String posicion, int puntos,
                        List<Reliquia> enInventario, List<Reliquia> entregadas, long tiempoSegundos) {
        this.jugador = jugador;
        this.posicion = posicion.toUpperCase();
        this.puntos = puntos;
        this.inventario = new Inventario(enInventario);
        this.entregadas = new ArrayList<>();
        if (entregadas != null) {
            this.entregadas.addAll(entregadas);
        }
        this.tiempoConsumidoSegundos = tiempoSegundos;
    }

    public String getJugadorId() {
        return jugador.getJugadorId();
    }

    public String getNombre() {
        return jugador.getNombre();
    }

    public String getAlias() {
        return jugador.getAlias();
    }

    public String getPosicion() {
        return posicion;
    }

    public void moverA(String nuevaPosicion) {
        this.posicion = nuevaPosicion.toUpperCase();
    }

    public int getPuntos() {
        return puntos;
    }

    public boolean tieneInventarioLleno() {
        return inventario.estaLleno();
    }

    public boolean tieneReliquiasParaEntregar() {
        return !inventario.estaVacio();
    }

    public void recogerReliquia(Reliquia reliquia) throws ReglaJuegoException {
        inventario.agregarReliquia(reliquia);
    }

    public int entregarReliquiasEnCampamento() throws ReglaJuegoException {
        List<Reliquia> lista = inventario.vaciarParaEntrega();
        int ganados = 0;
        for (Reliquia r : lista) {
            ganados += r.getValor();
            entregadas.add(r);
        }
        this.puntos += ganados;
        return ganados;
    }

    public List<Reliquia> getReliquiasInventario() {
        return inventario.getReliquias();
    }

    public List<Reliquia> getReliquiasEntregadas() {
        return entregadas;
    }

    public int getCantidadEntregadas() {
        return entregadas.size();
    }

    public long getTiempoConsumidoSegundos() {
        return tiempoConsumidoSegundos;
    }

    public void sumarTiempoTurno(long segundos) {
        if (segundos > 0) {
            this.tiempoConsumidoSegundos += segundos;
        }
    }
}