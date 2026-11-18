package ve.edu.ucab.poo.modelo;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import java.util.*;

public class Partida {
    private String partidaId;
    private String cofreId;
    private String campamentoCoord;
    private String cofreCoord;
    private Tablero tablero;
    private List<Participante> participantes;
    private Map<String, Participante> mapaParticipantes;
    private Set<String> templosExplorados;
    private Map<Integer, Pista> pistasDescubiertas;

    private EstadoPartida estado;
    private int ronda;
    private int indiceJugadorActual;
    private int accionesRestantes;
    private boolean cofreAbierto;
    private List<String> ganadores;
    private int revisionActual;

    public Partida(String partidaId, String cofreId, String campamentoCoord, String cofreCoord,
                   Tablero tablero, List<Participante> participantes, int revisionActual) {
        this.partidaId = partidaId;
        this.cofreId = cofreId;
        this.campamentoCoord = campamentoCoord.toUpperCase();
        this.cofreCoord = cofreCoord.toUpperCase();
        this.tablero = tablero;
        this.participantes = new ArrayList<>(participantes);
        this.mapaParticipantes = new HashMap<>();
        for (Participante p : participantes) {
            this.mapaParticipantes.put(p.getJugadorId(), p);
        }
        this.templosExplorados = new LinkedHashSet<>();
        this.pistasDescubiertas = new TreeMap<>();
        this.estado = EstadoPartida.EN_CURSO;
        this.ronda = 1;
        this.indiceJugadorActual = 0;
        this.accionesRestantes = ReglasConstantes.PUNTOS_ACCION_POR_TURNO;
        this.cofreAbierto = false;
        this.ganadores = new ArrayList<>();
        this.revisionActual = revisionActual;
    }

    public void restaurarEstadoSnapshot(EstadoPartida estado, int ronda, int indiceJugadorActual,
                                        int accionesRestantes, Collection<String> explorados,
                                        Collection<Pista> pistas, boolean cofreAbierto,
                                        Collection<String> ganadores, int revision) {
        this.estado = estado;
        this.ronda = ronda;
        this.indiceJugadorActual = indiceJugadorActual;
        this.accionesRestantes = accionesRestantes;
        this.templosExplorados.clear();
        if (explorados != null) {
            for (String c : explorados) {
                this.templosExplorados.add(c.toUpperCase());
            }
        }
        this.pistasDescubiertas.clear();
        if (pistas != null) {
            for (Pista p : pistas) {
                this.pistasDescubiertas.put(p.getIndice(), p);
            }
        }
        this.cofreAbierto = cofreAbierto;
        this.ganadores.clear();
        if (ganadores != null) {
            this.ganadores.addAll(ganadores);
        }
        this.revisionActual = revision;
    }

    public Participante getParticipanteActual() {
        return participantes.get(indiceJugadorActual);
    }

    public Participante buscarParticipantePorId(String id) {
        return mapaParticipantes.get(id);
    }

    public void verificarPartidaActiva() throws ReglaJuegoException {
        if (estado == EstadoPartida.FINALIZADA) {
            throw new ReglaJuegoException("La partida ya ha finalizado.");
        }
    }

    public void verificarPresupuesto(int costo) throws ReglaJuegoException {
        if (costo > accionesRestantes) {
            throw new ReglaJuegoException("Presupuesto insuficiente: cuesta " + costo + " y quedan " + accionesRestantes + ".");
        }
    }

    public void descontarAcciones(int costo) {
        this.accionesRestantes = Math.max(0, this.accionesRestantes - costo);
    }

    public boolean esTemploExplorado(String coord) {
        return templosExplorados.contains(coord.toUpperCase());
    }

    public void registrarTemploExplorado(String coord, Pista pista) {
        templosExplorados.add(coord.toUpperCase());
        if (pista != null) {
            pistasDescubiertas.put(pista.getIndice(), pista);
        }
    }

    public boolean estanAmbasPistasDescubiertas() {
        return pistasDescubiertas.containsKey(1) && pistasDescubiertas.containsKey(2);
    }

    public void marcarCofreAbierto() {
        this.cofreAbierto = true;
    }

    public void cerrarTurnoActual(long segundos) {
        if (estado == EstadoPartida.FINALIZADA) {
            return;
        }
        getParticipanteActual().sumarTiempoTurno(segundos);

        if (indiceJugadorActual == 0) {
            indiceJugadorActual = 1;
            accionesRestantes = ReglasConstantes.PUNTOS_ACCION_POR_TURNO;
        } else {
            if (ronda >= ReglasConstantes.TOTAL_RONDAS) {
                finalizarPartida();
            } else {
                ronda++;
                indiceJugadorActual = 0;
                accionesRestantes = ReglasConstantes.PUNTOS_ACCION_POR_TURNO;
            }
        }
    }

    private void finalizarPartida() {
        this.estado = EstadoPartida.FINALIZADA;
        this.ronda = ReglasConstantes.TOTAL_RONDAS;
        this.accionesRestantes = 0;
        this.ganadores.clear();

        Participante p1 = participantes.get(0);
        Participante p2 = participantes.get(1);

        if (p1.getPuntos() > p2.getPuntos()) {
            ganadores.add(p1.getJugadorId());
        } else if (p2.getPuntos() > p1.getPuntos()) {
            ganadores.add(p2.getJugadorId());
        } else {
            if (p1.getCantidadEntregadas() > p2.getCantidadEntregadas()) {
                ganadores.add(p1.getJugadorId());
            } else if (p2.getCantidadEntregadas() > p1.getCantidadEntregadas()) {
                ganadores.add(p2.getJugadorId());
            } else {
                ganadores.add(p1.getJugadorId());
                ganadores.add(p2.getJugadorId());
            }
        }
    }

    public String getPartidaId() { return partidaId; }
    public String getCofreId() { return cofreId; }
    public String getCampamentoCoord() { return campamentoCoord; }
    public String getCofreCoord() { return cofreCoord; }
    public Tablero getTablero() { return tablero; }
    public List<Participante> getParticipantes() { return participantes; }
    public Set<String> getTemplosExplorados() { return templosExplorados; }
    public Collection<Pista> getPistasDescubiertas() { return pistasDescubiertas.values(); }
    public Pista getPista(int indice) { return pistasDescubiertas.get(indice); }
    public EstadoPartida getEstado() { return estado; }
    public int getRonda() { return ronda; }
    public int getIndiceJugadorActual() { return indiceJugadorActual; }
    public int getAccionesRestantes() { return accionesRestantes; }
    public boolean isCofreAbierto() { return cofreAbierto; }
    public List<String> getGanadores() { return ganadores; }
    public int getRevisionActual() { return revisionActual; }
    public void setRevisionActual(int revisionActual) { this.revisionActual = revisionActual; }
}