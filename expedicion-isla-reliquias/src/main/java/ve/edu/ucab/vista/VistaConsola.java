package ve.edu.ucab.poo.vista;

import ve.edu.ucab.poo.acciones.ResultadoAccion;
import ve.edu.ucab.poo.cliente.ApiComunicacionException;
import ve.edu.ucab.poo.cliente.ClienteApi;
import ve.edu.ucab.poo.controlador.ControladorExpedicion;
import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Vista de consola para manejar los menus y mostrar el tablero.
 */
public class VistaConsola {
    private ControladorExpedicion controlador;
    private Scanner teclado;

    /**
     * Inicializa la vista con su controlador.
     */
    public VistaConsola(ControladorExpedicion controlador) {
        this.controlador = controlador;
        this.teclado = new Scanner(System.in);
    }

    /**
     * Muestra el menu principal y lee lo que elija el usuario.
     */
    public void iniciar() {
        System.out.println("=== EXPEDICION: LA ISLA DE LAS RELIQUIAS ===");

        if (!controlador.comprobarApiDisponible()) {
            System.out.println("Ojo: no hay conexion con la API en localhost:8000");
        } else {
            System.out.println("Conexion lista con la API (Contrato v2).");
        }

        boolean salir = false;
        while (!salir) {
            System.out.println("\nMenu principal:");
            System.out.println("1. Registrar jugador");
            System.out.println("2. Listar jugadores y estadisticas");
            System.out.println("3. Crear nueva partida");
            System.out.println("4. Cargar partida desde la API");
            System.out.println("5. Ayuda");
            System.out.println("6. Salir");
            System.out.print("Elige una opcion: ");

            String op = teclado.nextLine().trim();
            if (op.equals("1")) {
                registrarJugador();
            } else if (op.equals("2")) {
                listarJugadores();
            } else if (op.equals("3")) {
                crearPartida();
            } else if (op.equals("4")) {
                cargarPartida();
            } else if (op.equals("5")) {
                mostrarAyuda();
            } else if (op.equals("6")) {
                salir = true;
            } else {
                System.out.println("Esa opcion no existe, intenta de nuevo.");
            }
        }
    }

    private void mostrarAyuda() {
        System.out.println("\n--- Como se juega ---");
        System.out.println("Son 8 rondas y cada jugador tiene 3 acciones por turno.");
        System.out.println("Solo puedes moverte arriba, abajo, izquierda o derecha (1 casilla).");
        System.out.println("Sendero (S), Templo (T) y Campamento (C) cuestan 1 accion. Pantano (P) cuesta 2.");
        System.out.println("Puedes cargar hasta 2 reliquias y entregarlas en C (cuesta 1 accion).");
        System.out.println("Para abrir el cofre (X) necesitas las 2 pistas y escribir PISTA1-PISTA2 (cuesta 1 accion).");
        System.out.println("Si fallas la clave del cofre igual te quita 1 accion.");
    }

    private void registrarJugador() {
        try {
            System.out.print("Nombre: ");
            String nombre = teclado.nextLine();
            System.out.print("Alias: ");
            String alias = teclado.nextLine();

            Jugador j = controlador.registrarJugador(nombre, alias);
            System.out.println("Listo, jugador creado con ID: " + j.getJugadorId());
        } catch (ReglaJuegoException | ApiComunicacionException e) {
            System.out.println("No se pudo registrar: " + e.getMessage());
        }
    }

    private void listarJugadores() {
        try {
            System.out.print("Buscar por alias (Enter para ver todos): ");
            String filtro = teclado.nextLine();
            List<Jugador> jugadores = controlador.listarJugadores(filtro);

            if (jugadores.isEmpty()) {
                System.out.println("No hay jugadores todavia.");
                return;
            }

            System.out.println("\nLista de jugadores:");
            for (Jugador j : jugadores) {
                ClienteApi.EstadisticasJugadorDTO est = controlador.consultarEstadisticas(j.getJugadorId());
                System.out.println("- [" + est.jugadorId() + "] " + est.nombre() + " (" + est.alias() + ")"
                        + " | Partidas: " + est.partidasFinalizadas()
                        + " | Victorias: " + est.victorias()
                        + " | Puntos: " + est.puntosAcumulados()
                        + " | Reliquias: " + est.reliquiasEntregadas());
            }
        } catch (ApiComunicacionException e) {
            System.out.println("Error consultando jugadores: " + e.getMessage());
        }
    }

    private void crearPartida() {
        try {
            List<Jugador> lista = controlador.listarJugadores(null);
            if (lista.size() < 2) {
                System.out.println("Hacen falta al menos 2 jugadores registrados.");
                return;
            }

            for (Jugador j : lista) {
                System.out.println("  " + j.getJugadorId() + " -> " + j.getNombre() + " (" + j.getAlias() + ")");
            }

            System.out.print("ID del jugador 1: ");
            String j1 = teclado.nextLine().trim().toUpperCase();
            System.out.print("ID del jugador 2: ");
            String j2 = teclado.nextLine().trim().toUpperCase();

            Partida p = controlador.iniciarNuevaPartida(j1, j2);
            System.out.println("Partida creada: " + p.getPartidaId());
            jugarPartida();
        } catch (ReglaJuegoException | ApiComunicacionException e) {
            System.out.println("Error creando partida: " + e.getMessage());
        }
    }

    private void cargarPartida() {
        try {
            System.out.print("Filtrar por ID de jugador (Enter para ver todas): ");
            String filtro = teclado.nextLine().trim().toUpperCase();
            List<ClienteApi.ResumenPartidaDTO> partidas = controlador.listarPartidas(filtro.isEmpty() ? null : filtro);

            if (partidas.isEmpty()) {
                System.out.println("No hay partidas para mostrar.");
                return;
            }

            for (ClienteApi.ResumenPartidaDTO p : partidas) {
                System.out.println("  " + p.partidaId() + " | Jugadores: " + p.jugadores()
                        + " | Estado: " + p.estado() + " | Ronda: " + p.ronda());
            }

            System.out.print("Escribe el ID de la partida: ");
            String id = teclado.nextLine().trim().toUpperCase();
            if (id.isEmpty()) {
                return;
            }

            controlador.cargarPartidaExistente(id);
            jugarPartida();
        } catch (ApiComunicacionException e) {
            System.out.println("Error cargando partida: " + e.getMessage());
        }
    }

    private void jugarPartida() {
        boolean volver = false;

        while (!volver) {
            Partida p = controlador.getPartidaActiva();
            if (p.getEstado() == EstadoPartida.FINALIZADA) {
                mostrarResumenFinal(p);
                return;
            }

            if (controlador.isEstadoPendientePorSincronizar()) {
                volver = menuReintento(p.getPartidaId());
                continue;
            }

            mostrarEstadoTurno(p);

            System.out.println("\nQue quieres hacer?");
            System.out.println("1. Mover");
            System.out.println("2. Explorar templo");
            System.out.println("3. Entregar reliquias");
            System.out.println("4. Abrir cofre");
            System.out.println("5. Terminar turno");
            System.out.println("6. Guardar partida");
            System.out.println("7. Guardar y salir al menu");
            System.out.print("Opcion: ");

            String op = teclado.nextLine().trim();
            try {
                if (op.equals("1")) {
                    System.out.print("Coordenada destino (ej. B3): ");
                    String dest = teclado.nextLine();
                    ResultadoAccion res = controlador.mover(dest);
                    System.out.println(res.getMensaje());
                    guardarDespuesDeAccion(res.getPuntosConsumidos());
                } else if (op.equals("2")) {
                    ResultadoAccion res = controlador.explorarTemplo();
                    System.out.println(res.getMensaje());
                    guardarDespuesDeAccion(res.getPuntosConsumidos());
                } else if (op.equals("3")) {
                    ResultadoAccion res = controlador.entregarReliquias();
                    System.out.println(res.getMensaje());
                    guardarDespuesDeAccion(res.getPuntosConsumidos());
                } else if (op.equals("4")) {
                    String clave = "";
                    if (!p.isCofreAbierto()) {
                        System.out.print("Clave del cofre (PISTA1-PISTA2): ");
                        clave = teclado.nextLine();
                    }
                    ResultadoAccion res = controlador.abrirCofre(clave);
                    System.out.println(res.getMensaje());
                    guardarDespuesDeAccion(res.getPuntosConsumidos());
                } else if (op.equals("5")) {
                    controlador.terminarTurnoYSincronizar();
                    System.out.println("Turno cerrado y guardado.");
                } else if (op.equals("6")) {
                    controlador.guardarPartidaManual();
                    System.out.println("Partida guardada.");
                } else if (op.equals("7")) {
                    controlador.guardarPartidaManual();
                    System.out.println("Partida guardada.");
                    volver = true;
                } else {
                    System.out.println("Opcion no valida.");
                }
            } catch (ReglaJuegoException e) {
                System.out.println("No se puede hacer eso: " + e.getMessage());
            } catch (ApiComunicacionException e) {
                System.out.println("Fallo al guardar en la API: " + e.getMessage());
            }
        }
    }

    private void guardarDespuesDeAccion(int costoAccion) throws ApiComunicacionException {
        Partida p = controlador.getPartidaActiva();
        if (p.getAccionesRestantes() <= 0) {
            System.out.println("Se acabaron las acciones del turno, cerrando...");
            controlador.terminarTurnoYSincronizar();
        } else if (costoAccion > 0) {
            controlador.guardarPartidaManual();
        }
    }

    private boolean menuReintento(String partidaId) {
        System.out.println("\nQuedo un guardado pendiente con el servidor:");
        System.out.println("1. Reintentar guardar");
        System.out.println("2. Recargar partida desde el servidor");
        System.out.println("3. Volver al menu principal");
        System.out.print("Opcion: ");

        String op = teclado.nextLine().trim();
        try {
            if (op.equals("1")) {
                controlador.reintentarGuardadoPendiente();
                System.out.println("Guardado listo.");
            } else if (op.equals("2")) {
                controlador.cargarPartidaExistente(partidaId);
                System.out.println("Partida recargada.");
            } else if (op.equals("3")) {
                return true;
            }
        } catch (ApiComunicacionException e) {
            System.out.println("Sigue fallando la conexion: " + e.getMessage());
        }
        return false;
    }

    private void mostrarEstadoTurno(Partida p) {
        Participante actual = p.getParticipanteActual();
        Participante j1 = p.getParticipantes().get(0);
        Participante j2 = p.getParticipantes().get(1);

        System.out.println("\n--------------------------------------------------");
        System.out.println("Partida " + p.getPartidaId() + " | Ronda " + p.getRonda() + "/" + ReglasConstantes.TOTAL_RONDAS
                + " | Turno de: " + actual.getJugadorId() + " (" + actual.getNombre() + ")"
                + " | Acciones restantes: " + p.getAccionesRestantes());

        System.out.println(j1.getJugadorId() + " -> Pos: " + j1.getPosicion() + " | Pts: " + j1.getPuntos()
                + " | Inv (" + j1.getReliquiasInventario().size() + "/2): " + verInventario(j1.getReliquiasInventario()));
        System.out.println(j2.getJugadorId() + " -> Pos: " + j2.getPosicion() + " | Pts: " + j2.getPuntos()
                + " | Inv (" + j2.getReliquiasInventario().size() + "/2): " + verInventario(j2.getReliquiasInventario()));

        Pista p1 = p.getPista(1);
        Pista p2 = p.getPista(2);
        String t1 = p1 != null ? p1.getFragmento() : "(sin descubrir)";
        String t2 = p2 != null ? p2.getFragmento() : "(sin descubrir)";
        System.out.println("Pista 1: " + t1 + " | Pista 2: " + t2 + " | Cofre: " + (p.isCofreAbierto() ? "ABIERTO" : "CERRADO") + "\n");

        imprimirTablero(p);
    }

    private void imprimirTablero(Partida p) {
        Tablero tab = p.getTablero();
        Participante j1 = p.getParticipantes().get(0);
        Participante j2 = p.getParticipantes().get(1);

        StringBuilder cab = new StringBuilder("    ");
        for (int c = 0; c < tab.getColumnas(); c++) {
            char colLetra = (char) ('A' + c);
            cab.append(String.format("%-9s", "  " + colLetra));
        }
        System.out.println(cab);

        for (int f = 0; f < tab.getFilas(); f++) {
            StringBuilder filaTexto = new StringBuilder(" " + (f + 1) + "  ");
            for (int c = 0; c < tab.getColumnas(); c++) {
                Casilla cas = tab.obtenerCasillaPorIndice(f, c);
                String coord = cas.getCoordenada();
                String celda = cas.getTerreno().getSimbolo();

                List<String> quienes = new ArrayList<>();
                if (j1.getPosicion().equalsIgnoreCase(coord)) {
                    quienes.add(j1.getJugadorId());
                }
                if (j2.getPosicion().equalsIgnoreCase(coord)) {
                    quienes.add(j2.getJugadorId());
                }
                if (!quienes.isEmpty()) {
                    celda += ":" + String.join(",", quienes);
                }
                filaTexto.append(String.format("%-9s", "[" + celda + "]"));
            }
            System.out.println(filaTexto);
        }
    }

    private String verInventario(List<Reliquia> lista) {
        if (lista.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            Reliquia r = lista.get(i);
            sb.append(r.getReliquiaId()).append("(").append(r.getValor()).append(")");
            if (i < lista.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private void mostrarResumenFinal(Partida p) {
        System.out.println("\n=== PARTIDA TERMINADA (" + p.getPartidaId() + ") ===");
        for (Participante part : p.getParticipantes()) {
            System.out.println(part.getJugadorId() + " (" + part.getNombre() + "): "
                    + part.getPuntos() + " pts | Entregadas: " + part.getCantidadEntregadas()
                    + " | Tiempo: " + part.getTiempoConsumidoSegundos() + "s");
        }
        if (p.getGanadores().size() > 1) {
            System.out.println("Empate entre: " + String.join(" y ", p.getGanadores()));
        } else if (p.getGanadores().size() == 1) {
            System.out.println("Ganador: " + p.getGanadores().get(0));
        }
    }
}