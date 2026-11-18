package ve.edu.ucab.poo.acciones;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.Partida;

public interface AccionJuego {
    int calcularCosto(Partida partida) throws ReglaJuegoException;
    void validar(Partida partida) throws ReglaJuegoException;
    ResultadoAccion ejecutar(Partida partida) throws ReglaJuegoException;
}