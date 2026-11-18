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

public class VistaConsola {
    private ControladorExpedicion controlador;
    private Scanner scanner;

    public VistaConsola(ControladorExpedicion controlador) {
        this.controlador = controlador;
        this.scanner = new Scanner(System.in);
    }

    public void iniciar() {
        System.out.println("--- EXPEDICION: LA ISLA DE LAS RELIQUIAS ---");

        if (!controlador.comprobarApiDisponible()) {
            System.out.println("Aviso: No se pudo conectar con la API en localhost:8000");
        } else {
            System.out.println("Conectado a la API REST (Contrato v2).");
        }

        boolean salir = false;
        while (!salir) {
            System.out.println("\nMENU PRINCIPAL");
            System.out.println("1. Registrar jugador");
            System.out.println("2. Listar jugadores y estadisticas");
            System.out.println("3. Crear nueva partida");
            System.out.println("4. Cargar partida desde la API");
            System.out.println("5. Salir");
            System.out.print("Opcion: ");

            String opcion = scanner.nextLine().trim();
            switch (opcion) {
                case "1":
                    registrarJugador();
                    break;
                case "2":
                    listarJugadores();
                    break;
                case "3":
                    crearPartida();
                    break;
                case "4":
                    cargarPartida();
                    break;
                case "5":
                    salir = true;
                    break;
                default:
                    System.out.println("Opcion invalida.");
            }
        }
    }

    private void registrarJugador() {
        try {
            System.out.print("Nombre del jugador: ");
            String nombre = scanner.nextLine();
            System.out.print("Alias: ");
            String alias = scanner.nextLine();

            Jugador j = controlador.registrarJugador(nombre, alias);
            System.out.println("Jugador registrado con ID: " + j.getJugadorId());
        } catch (ReglaJuegoException | ApiComunicacionException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void listarJugadores() {
        try {
            System.out.print("Filtrar por alias (Enter para ver todos): ");
            String filtro = scanner.nextLine();
            List<Jugador> lista = controlador.listarJugadores(filtro);

            if (lista.isEmpty()) {
                System.out.println("No hay jugadores registrados.");
                return;
            }

            System.out.println("\nJugadores:");
            for (Jugador j : lista) {
                ClienteApi.EstadisticasJugadorDTO est = controlador.consultarEstadisticas(j.getJugadorId());
                System.out.println("- [" + est.jugadorId() + "] " + est.nombre() + " (@" + est.alias() + ")"
                        + " | Partidas: " + est.partidasFinalizadas()
                        + " | Victorias: " + est.victorias()
                        + " | Puntos: " + est.puntosAcumulados()
                        + " | Reliquias: " + est.reliquiasEntregadas());
            }
        } catch (ApiComunicacionException e) {
            System.out.println("Error de red: " + e.getMessage());
        }
    }

    private void crearPartida() {
        try {
            List<Jugador> disp = controlador.listarJugadores(null);
            if (disp.size() < 2) {
                System.out.println("Se necesitan al menos 2 jugadores registrados.");
                return;
            }

            for (Jugador j : disp) {
                System.out.println("  " + j.getJugadorId() + " - " + j.getNombre() + " (@" + j.getAlias() + ")");
            }

            System.out.print("ID Jugador 1: ");
            String j1 = scanner.nextLine().trim().toUpperCase();
            System.out.print("ID Jugador 2: ");
            String j2 = scanner.nextLine().trim().toUpperCase();

            Partida p = controlador.iniciarNuevaPartida(j1, j2);
            System.out.println("Partida creada: " + p.getPartidaId());
            jugarPartida();
        } catch (ReglaJuegoException | ApiComunicacionException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void cargarPartida() {
        try {
            System.out.print("Filtrar por jugadorId (Enter para todas): ");
            String filtro = scanner.nextLine().trim().toUpperCase();
            List<ClienteApi.ResumenPartidaDTO> partidas = controlador.listarPartidas(filtro.isEmpty() ? null : filtro);

            if (partidas.isEmpty()) {
                System.out.println("No se encontraron partidas.");
                return;
            }

            for (ClienteApi.ResumenPartidaDTO p : partidas) {
                System.out.println("  ID: " + p.partidaId() + " | Jugadores: " + p.jugadores()
                        + " | Estado: " + p.estado() + " | Ronda: " + p.ronda());
            }

            System.out.print("ID de la partida a cargar: ");
            String id = scanner.nextLine().trim().toUpperCase();
            if (id.isEmpty()) return;

            controlador.cargarPartidaExistente(id);
            jugarPartida();
        } catch (ApiComunicacionException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void jugarPartida() {
        boolean salirMenu = false;

        while (!salirMenu) {
            Partida p = controlador.getPartidaActiva();
            if (p.getEstado() == EstadoPartida.FINALIZADA) {
                mostrarResumenFinal(p);
                return;
            }

            if (controlador.isEstadoPendientePorSincronizar()) {
                salirMenu = menuReintento(p.getPartidaId());
                continue;
            }

            mostrarEstadoTurno(p);

            System.out.println("\nAcciones:");
            System.out.println("1. Mover");
            System.out.println("2. Explorar templo");
            System.out.println("3. Entregar reliquias");
            System.out.println("4. Abrir cofre");
            System.out.println("5. Terminar turno");
            System.out.println("6. Guardar y salir al menu");
            System.out.print("Accion: ");

            String op = scanner.nextLine().trim();
            try {
                switch (op) {
                    case "1":
                        System.out.print("Coordenada destino: ");
                        String dest = scanner.nextLine();
                        ResultadoAccion r1 = controlador.mover(dest);
                        System.out.println(r1.getMensaje());
                        verificarFinAcciones();
                        break;
                    case "2":
                        ResultadoAccion r2 = controlador.explorarTemplo();
                        System.out.println(r2.getMensaje());
                        verificarFinAcciones();
                        break;
                    case "3":
                        ResultadoAccion r3 = controlador.entregarReliquias();
                        System.out.println(r3.getMensaje());
                        verificarFinAcciones();
                        break;
                    case "4":
                        String pass = "";
                        if (!p.isCofreAbierto()) {
                            System.out.print("Contrasena del cofre (PISTA1-PISTA2): ");
                            pass = scanner.nextLine();
                        }
                        ResultadoAccion r4 = controlador.abrirCofre(pass);
                        System.out.println(r4.getMensaje());
                        verificarFinAcciones();
                        break;
                    case "5":
                        controlador.terminarTurnoYSincronizar();
                        System.out.println("Turno finalizado y guardado.");
                        break;
                    case "6":
                        controlador.guardarPartidaManual();
                        System.out.println("Partida guardada.");
                        salirMenu = true;
                        break;
                    default:
                        System.out.println("Opcion invalida.");
                }
            } catch (ReglaJuegoException e) {
                System.out.println("Accion invalida: " + e.getMessage());
            } catch (ApiComunicacionException e) {
                System.out.println("Error al sincronizar con la API: " + e.getMessage());
            }
        }
    }

    private void verificarFinAcciones() throws ApiComunicacionException {
        Partida p = controlador.getPartidaActiva();
        if (p.getAccionesRestantes() <= 0) {
            System.out.println("Sin acciones restantes. Cerrando turno...");
            controlador.terminarTurnoYSincronizar();
        }
    }

    private boolean menuReintento(String partidaId) {
        System.out.println("\nGuardado pendiente:");
        System.out.println("1. Reintentar guardar");
        System.out.println("2. Recargar partida desde el servidor");
        System.out.println("3. Volver al menu principal");
        System.out.print("Opcion: ");
        String op = scanner.nextLine().trim();
        try {
            if (op.equals("1")) {
                controlador.reintentarGuardadoPendiente();
                System.out.println("Guardado exitoso.");
            } else if (op.equals("2")) {
                controlador.cargarPartidaExistente(partidaId);
                System.out.println("Partida recargada.");
            } else if (op.equals("3")) {
                return true;
            }
        } catch (ApiComunicacionException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return false;
    }

    private void mostrarEstadoTurno(Partida p) {
        Participante actual = p.getParticipanteActual();
        Participante p1 = p.getParticipantes().get(0);
        Participante p2 = p.getParticipantes().get(1);

        System.out.println("\n--------------------------------------------------");
        System.out.println("Partida: " + p.getPartidaId() + " | Ronda: " + p.getRonda() + "/" + ReglasConstantes.TOTAL_RONDAS
                + " | Turno: " + actual.getJugadorId() + " (" + actual.getNombre() + ") | Acciones: " + p.getAccionesRestantes());

        System.out.println(p1.getJugadorId() + " -> Pos: " + p1.getPosicion() + " | Puntos: " + p1.getPuntos()
                + " | Inv (" + p1.getReliquiasInventario().size() + "/2): " + textoInventario(p1.getReliquiasInventario()));
        System.out.println(p2.getJugadorId() + " -> Pos: " + p2.getPosicion() + " | Puntos: " + p2.getPuntos()
                + " | Inv (" + p2.getReliquiasInventario().size() + "/2): " + textoInventario(p2.getReliquiasInventario()));

        Pista pista1 = p.getPista(1);
        Pista pista2 = p.getPista(2);
        String s1 = pista1 != null ? pista1.getFragmento() : "(pendiente)";
        String s2 = pista2 != null ? pista2.getFragmento() : "(pendiente)";
        System.out.println("Pista 1: " + s1 + " | Pista 2: " + s2 + " | Cofre: " + (p.isCofreAbierto() ? "ABIERTO" : "CERRADO") + "\n");

        imprimirTablero(p);
    }

    private void imprimirTablero(Partida p) {
        Tablero tab = p.getTablero();
        Participante j1 = p.getParticipantes().get(0);
        Participante j2 = p.getParticipantes().get(1);

        StringBuilder cab = new StringBuilder("    ");
        for (int c = 0; c < tab.getColumnas(); c++) {
            char letra = (char) ('A' + c);
            cab.append(String.format("%-9s", "  " + letra));
        }
        System.out.println(cab);

        for (int f = 0; f < tab.getFilas(); f++) {
            StringBuilder filaStr = new StringBuilder(" " + (f + 1) + "  ");
            for (int c = 0; c < tab.getColumnas(); c++) {
                Casilla cas = tab.obtenerCasillaPorIndice(f, c);
                String coord = cas.getCoordenada();
                String etiq = cas.getTerreno().getSimbolo();

                List<String> ocup = new ArrayList<>();
                if (j1.getPosicion().equalsIgnoreCase(coord)) ocup.add(j1.getJugadorId());
                if (j2.getPosicion().equalsIgnoreCase(coord)) ocup.add(j2.getJugadorId());

                if (!ocup.isEmpty()) {
                    etiq += ":" + String.join(",", ocup);
                }
                filaStr.append(String.format("%-9s", "[" + etiq + "]"));
            }
            System.out.println(filaStr);
        }
    }

    private String textoInventario(List<Reliquia> lista) {
        if (lista.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            Reliquia r = lista.get(i);
            sb.append(r.getReliquiaId()).append("(").append(r.getValor()).append(")");
            if (i < lista.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private void mostrarResumenFinal(Partida p) {
        System.out.println("\n--- FIN DE LA PARTIDA " + p.getPartidaId() + " ---");
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