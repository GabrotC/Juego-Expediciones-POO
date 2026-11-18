package ve.edu.ucab.poo.controlador;

import ve.edu.ucab.poo.acciones.AccionJuego;
import ve.edu.ucab.poo.acciones.FabricaAcciones;
import ve.edu.ucab.poo.acciones.ResultadoAccion;
import ve.edu.ucab.poo.cliente.ApiComunicacionException;
import ve.edu.ucab.poo.cliente.ClienteApi;
import ve.edu.ucab.poo.criptografia.ServicioCriptografico;
import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.Jugador;
import ve.edu.ucab.poo.modelo.Partida;
import ve.edu.ucab.poo.modelo.ReglasConstantes;

import java.util.List;
import java.util.UUID;

public class ControladorExpedicion {
    private ClienteApi clienteApi;
    private FabricaAcciones fabricaAcciones;
    private Partida partidaActiva;
    private long inicioTurnoMillis;
    private boolean estadoPendientePorSincronizar;

    public ControladorExpedicion(ClienteApi clienteApi) {
        this.clienteApi = clienteApi;
        this.fabricaAcciones = new FabricaAcciones(new ServicioCriptografico());
        this.estadoPendientePorSincronizar = false;
    }

    public boolean comprobarApiDisponible() {
        return clienteApi.verificarSaludContratoV2();
    }

    public Jugador registrarJugador(String nombre, String alias) throws ReglaJuegoException, ApiComunicacionException {
        String nom = nombre != null ? nombre.trim() : "";
        String al = alias != null ? alias.trim() : "";

        if (!ReglasConstantes.PATRON_NOMBRE_JUGADOR.matcher(nom).matches()) {
            throw new ReglaJuegoException("Nombre invalido: debe tener entre 1 y 80 caracteres.");
        }
        if (!ReglasConstantes.PATRON_ALIAS_JUGADOR.matcher(al).matches()) {
            throw new ReglaJuegoException("Alias invalido: debe tener de 3 a 20 caracteres validos.");
        }
        return clienteApi.registrarJugador(nom, al);
    }

    public List<Jugador> listarJugadores(String filtroAlias) throws ApiComunicacionException {
        return clienteApi.listarJugadores(filtroAlias);
    }

    public ClienteApi.EstadisticasJugadorDTO consultarEstadisticas(String jugadorId) throws ApiComunicacionException {
        return clienteApi.consultarEstadisticas(jugadorId);
    }

    public List<ClienteApi.ResumenPartidaDTO> listarPartidas(String filtroJugadorId) throws ApiComunicacionException {
        return clienteApi.listarPartidas(filtroJugadorId);
    }

    public Partida iniciarNuevaPartida(String j1, String j2) throws ReglaJuegoException, ApiComunicacionException {
        if (j1 == null || j2 == null || j1.isBlank() || j2.isBlank()) {
            throw new ReglaJuegoException("Debe ingresar los dos jugadores.");
        }
        if (j1.trim().equalsIgnoreCase(j2.trim())) {
            throw new ReglaJuegoException("No se puede repetir el mismo jugador.");
        }
        String reqId = "req-" + UUID.randomUUID();
        this.partidaActiva = clienteApi.crearPartida(reqId, j1.trim(), j2.trim());
        this.inicioTurnoMillis = System.currentTimeMillis();
        this.estadoPendientePorSincronizar = false;
        return this.partidaActiva;
    }

    public Partida cargarPartidaExistente(String partidaId) throws ApiComunicacionException {
        this.partidaActiva = clienteApi.cargarPartida(partidaId);
        this.inicioTurnoMillis = System.currentTimeMillis();
        this.estadoPendientePorSincronizar = false;
        return this.partidaActiva;
    }

    public ResultadoAccion mover(String destino) throws ReglaJuegoException {
        verificarPendiente();
        AccionJuego accion = fabricaAcciones.crearAccionMover(destino);
        return accion.ejecutar(partidaActiva);
    }

    public ResultadoAccion explorarTemplo() throws ReglaJuegoException {
        verificarPendiente();
        AccionJuego accion = fabricaAcciones.crearAccionExplorar((pId, coord) -> {
            try {
                return clienteApi.consultarContenidoTemplo(pId, coord);
            } catch (ApiComunicacionException e) {
                throw new ReglaJuegoException("Error al consultar templo: " + e.getMessage());
            }
        });
        return accion.ejecutar(partidaActiva);
    }

    public ResultadoAccion entregarReliquias() throws ReglaJuegoException {
        verificarPendiente();
        AccionJuego accion = fabricaAcciones.crearAccionEntregar();
        return accion.ejecutar(partidaActiva);
    }

    public ResultadoAccion abrirCofre(String contrasena) throws ReglaJuegoException {
        verificarPendiente();
        AccionJuego accion = fabricaAcciones.crearAccionAbrirCofre(contrasena, pId -> {
            try {
                return clienteApi.obtenerPaqueteCofre(pId);
            } catch (ApiComunicacionException e) {
                throw new ReglaJuegoException("Error al obtener cofre: " + e.getMessage());
            }
        });
        return accion.ejecutar(partidaActiva);
    }

    public void terminarTurnoYSincronizar() throws ApiComunicacionException {
        if (!estadoPendientePorSincronizar) {
            long segs = Math.max(1L, (System.currentTimeMillis() - inicioTurnoMillis) / 1000L);
            partidaActiva.cerrarTurnoActual(segs);
            estadoPendientePorSincronizar = true;
        }
        clienteApi.guardarEstadoPartida(partidaActiva);
        estadoPendientePorSincronizar = false;
        inicioTurnoMillis = System.currentTimeMillis();
    }

    public void reintentarGuardadoPendiente() throws ApiComunicacionException {
        if (partidaActiva != null && estadoPendientePorSincronizar) {
            clienteApi.guardarEstadoPartida(partidaActiva);
            estadoPendientePorSincronizar = false;
            inicioTurnoMillis = System.currentTimeMillis();
        }
    }

    public void guardarPartidaManual() throws ApiComunicacionException {
        if (partidaActiva != null) {
            clienteApi.guardarEstadoPartida(partidaActiva);
            estadoPendientePorSincronizar = false;
        }
    }

    private void verificarPendiente() throws ReglaJuegoException {
        if (estadoPendientePorSincronizar) {
            throw new ReglaJuegoException("Hay un cierre de turno pendiente por guardar en el servidor.");
        }
    }

    public Partida getPartidaActiva() {
        return partidaActiva;
    }

    public boolean isEstadoPendientePorSincronizar() {
        return estadoPendientePorSincronizar;
    }
}