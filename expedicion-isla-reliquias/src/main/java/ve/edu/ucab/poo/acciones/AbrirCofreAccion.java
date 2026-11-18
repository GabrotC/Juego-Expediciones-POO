package ve.edu.ucab.poo.acciones;

import ve.edu.ucab.poo.criptografia.PaqueteCifradoCofre;
import ve.edu.ucab.poo.criptografia.ServicioCriptografico;
import ve.edu.ucab.poo.excepciones.AutenticacionCofreException;
import ve.edu.ucab.poo.excepciones.DatosCorruptosCofreException;
import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.*;

public class AbrirCofreAccion implements AccionJuego {

    public interface ProveedorPaqueteCofre {
        PaqueteCifradoCofre obtenerPaquete(String partidaId) throws ReglaJuegoException;
    }

    private String contrasenaIngresada;
    private ProveedorPaqueteCofre proveedorCofre;
    private ServicioCriptografico servicioCriptografico;

    public AbrirCofreAccion(String contrasenaIngresada,
                            ProveedorPaqueteCofre proveedorCofre,
                            ServicioCriptografico servicioCriptografico) {
        this.contrasenaIngresada = contrasenaIngresada;
        this.proveedorCofre = proveedorCofre;
        this.servicioCriptografico = servicioCriptografico;
    }

    @Override
    public int calcularCosto(Partida partida) {
        if (partida.isCofreAbierto()) {
            return 0;
        }
        return ReglasConstantes.COSTO_ABRIR_COFRE;
    }

    @Override
    public void validar(Partida partida) throws ReglaJuegoException {
        partida.verificarPartidaActiva();
        Participante actual = partida.getParticipanteActual();
        Casilla casilla = partida.getTablero().obtenerCasilla(actual.getPosicion());

        if (casilla.getTerreno() != TipoTerreno.TEMPLO_COFRE) {
            throw new ReglaJuegoException("Debe estar en la casilla del Cofre (X).");
        }
        if (partida.isCofreAbierto()) {
            return;
        }
        if (!partida.estanAmbasPistasDescubiertas()) {
            throw new ReglaJuegoException("Se deben conocer las dos pistas antes de abrir el cofre.");
        }
        if (actual.tieneInventarioLleno()) {
            throw new ReglaJuegoException("Regrese al campamento para entregar antes de abrir el cofre.");
        }
        partida.verificarPresupuesto(ReglasConstantes.COSTO_ABRIR_COFRE);
        servicioCriptografico.normalizarYValidarFormatoContrasena(contrasenaIngresada);
    }

    @Override
    public ResultadoAccion ejecutar(Partida partida) throws ReglaJuegoException {
        validar(partida);

        if (partida.isCofreAbierto()) {
            return new ResultadoAccion(true, 0, "Cofre ya esta ABIERTO y vacio. Acciones restantes: " + partida.getAccionesRestantes() + ".");
        }

        String clave = servicioCriptografico.normalizarYValidarFormatoContrasena(contrasenaIngresada);
        PaqueteCifradoCofre paquete = proveedorCofre.obtenerPaquete(partida.getPartidaId());

        try {
            Reliquia reliquia = servicioCriptografico.descifrarCofre(
                    paquete,
                    partida.getPartidaId(),
                    partida.getCofreId(),
                    clave
            );
            partida.getParticipanteActual().recogerReliquia(reliquia);
            partida.marcarCofreAbierto();
            partida.descontarAcciones(ReglasConstantes.COSTO_ABRIR_COFRE);

            return new ResultadoAccion(
                    true,
                    ReglasConstantes.COSTO_ABRIR_COFRE,
                    "Cofre abierto. " + reliquia.getReliquiaId() + " agregada al inventario (valor "
                            + reliquia.getValor() + "). Acciones restantes: " + partida.getAccionesRestantes()
            );
        } catch (AutenticacionCofreException e) {
            partida.descontarAcciones(ReglasConstantes.COSTO_ABRIR_COFRE);
            return new ResultadoAccion(
                    false,
                    ReglasConstantes.COSTO_ABRIR_COFRE,
                    "No se pudo autenticar el archivo. Acciones restantes: " + partida.getAccionesRestantes() + "."
            );
        } catch (DatosCorruptosCofreException e) {
            throw new ReglaJuegoException(e.getMessage());
        }
    }
}