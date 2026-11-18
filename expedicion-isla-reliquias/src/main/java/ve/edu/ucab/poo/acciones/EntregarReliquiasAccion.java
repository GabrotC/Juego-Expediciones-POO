package ve.edu.ucab.poo.acciones;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.*;

public class EntregarReliquiasAccion implements AccionJuego {

    @Override
    public int calcularCosto(Partida partida) {
        return ReglasConstantes.COSTO_ENTREGAR;
    }

    @Override
    public void validar(Partida partida) throws ReglaJuegoException {
        partida.verificarPartidaActiva();
        Participante actual = partida.getParticipanteActual();
        Casilla casilla = partida.getTablero().obtenerCasilla(actual.getPosicion());

        if (casilla.getTerreno() != TipoTerreno.CAMPAMENTO) {
            throw new ReglaJuegoException("Debe estar en el Campamento (C) para entregar reliquias.");
        }
        if (!actual.tieneReliquiasParaEntregar()) {
            throw new ReglaJuegoException("No lleva reliquias en el inventario para entregar.");
        }
        partida.verificarPresupuesto(ReglasConstantes.COSTO_ENTREGAR);
    }

    @Override
    public ResultadoAccion ejecutar(Partida partida) throws ReglaJuegoException {
        validar(partida);
        Participante actual = partida.getParticipanteActual();
        int puntos = actual.entregarReliquiasEnCampamento();
        partida.descontarAcciones(ReglasConstantes.COSTO_ENTREGAR);

        return new ResultadoAccion(
                true,
                ReglasConstantes.COSTO_ENTREGAR,
                "Entrega realizada (+" + puntos + " puntos). Puntuacion actual: "
                        + actual.getPuntos() + ". Acciones restantes: " + partida.getAccionesRestantes() + "."
        );
    }
}