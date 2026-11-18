package ve.edu.ucab.poo.acciones;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.*;

public class ExplorarTemploAccion implements AccionJuego {

    public interface ProveedorContenidoTemplo {
        DatosTemplo consultarTemplo(String partidaId, String coordenada) throws ReglaJuegoException;
    }

    public static class DatosTemplo {
        private Reliquia reliquia;
        private Pista pistaOpcional;

        public DatosTemplo(Reliquia reliquia, Pista pistaOpcional) {
            this.reliquia = reliquia;
            this.pistaOpcional = pistaOpcional;
        }

        public Reliquia reliquia() {
            return reliquia;
        }

        public Pista pistaOpcional() {
            return pistaOpcional;
        }
    }

    private ProveedorContenidoTemplo proveedor;

    public ExplorarTemploAccion(ProveedorContenidoTemplo proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public int calcularCosto(Partida partida) {
        String pos = partida.getParticipanteActual().getPosicion();
        if (partida.esTemploExplorado(pos)) {
            return 0;
        }
        return ReglasConstantes.COSTO_EXPLORAR;
    }

    @Override
    public void validar(Partida partida) throws ReglaJuegoException {
        partida.verificarPartidaActiva();
        Participante actual = partida.getParticipanteActual();
        Casilla casilla = partida.getTablero().obtenerCasilla(actual.getPosicion());

        if (casilla.getTerreno() != TipoTerreno.TEMPLO_NORMAL) {
            throw new ReglaJuegoException("Debe estar en un Templo normal (T) para explorar.");
        }
        if (partida.esTemploExplorado(actual.getPosicion())) {
            return;
        }
        if (actual.tieneInventarioLleno()) {
            throw new ReglaJuegoException("Regrese al campamento para entregar antes de explorar.");
        }
        partida.verificarPresupuesto(ReglasConstantes.COSTO_EXPLORAR);
    }

    @Override
    public ResultadoAccion ejecutar(Partida partida) throws ReglaJuegoException {
        validar(partida);
        Participante actual = partida.getParticipanteActual();
        String pos = actual.getPosicion();

        if (partida.esTemploExplorado(pos)) {
            return new ResultadoAccion(true, 0, "Explorado y vacio. Acciones restantes: " + partida.getAccionesRestantes() + ".");
        }

        DatosTemplo datos = proveedor.consultarTemplo(partida.getPartidaId(), pos);

        actual.recogerReliquia(datos.reliquia());
        partida.registrarTemploExplorado(pos, datos.pistaOpcional());
        partida.descontarAcciones(ReglasConstantes.COSTO_EXPLORAR);

        String msg = "Templo explorado en " + pos + ". Recogida "
                + datos.reliquia().getReliquiaId() + " (valor " + datos.reliquia().getValor() + ").";

        if (datos.pistaOpcional() != null) {
            msg += " Pista " + datos.pistaOpcional().getIndice()
                    + " descubierta: " + datos.pistaOpcional().getFragmento() + "!";
        }
        msg += " Acciones restantes: " + partida.getAccionesRestantes() + ".";

        return new ResultadoAccion(true, ReglasConstantes.COSTO_EXPLORAR, msg);
    }
}