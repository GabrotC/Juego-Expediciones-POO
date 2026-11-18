package ve.edu.ucab.poo.acciones;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.Casilla;
import ve.edu.ucab.poo.modelo.Participante;
import ve.edu.ucab.poo.modelo.Partida;

public class MoverAccion implements AccionJuego {
    private String coordenadaDestino;

    public MoverAccion(String coordenadaDestino) {
        this.coordenadaDestino = coordenadaDestino != null ? coordenadaDestino.trim().toUpperCase() : "";
    }

    @Override
    public int calcularCosto(Partida partida) throws ReglaJuegoException {
        Casilla destino = partida.getTablero().obtenerCasilla(coordenadaDestino);
        return destino.getCostoEntrada();
    }

    @Override
    public void validar(Partida partida) throws ReglaJuegoException {
        partida.verificarPartidaActiva();
        Participante actual = partida.getParticipanteActual();
        Casilla destino = partida.getTablero().obtenerCasilla(coordenadaDestino);

        if (!partida.getTablero().sonVecinosOrtogonales(actual.getPosicion(), destino.getCoordenada())) {
            throw new ReglaJuegoException("Movimiento invalido: no es vecino ortogonal de " + actual.getPosicion() + ".");
        }
        partida.verificarPresupuesto(destino.getCostoEntrada());
    }

    @Override
    public ResultadoAccion ejecutar(Partida partida) throws ReglaJuegoException {
        validar(partida);
        int costo = calcularCosto(partida);
        Participante actual = partida.getParticipanteActual();
        Casilla destino = partida.getTablero().obtenerCasilla(coordenadaDestino);

        actual.moverA(destino.getCoordenada());
        partida.descontarAcciones(costo);

        return new ResultadoAccion(
                true,
                costo,
                "Movimiento exitoso a " + destino.getCoordenada() + " (" + destino.getTerreno().getNombre()
                        + ", costo " + costo + "). Acciones restantes: " + partida.getAccionesRestantes() + "."
        );
    }
}