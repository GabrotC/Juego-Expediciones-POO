package ve.edu.ucab.poo.cliente;

import com.google.gson.*;
import ve.edu.ucab.poo.acciones.ExplorarTemploAccion;
import ve.edu.ucab.poo.criptografia.PaqueteCifradoCofre;
import ve.edu.ucab.poo.modelo.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class ClienteApi {
    private String baseUrl;
    private HttpClient httpClient;
    private Gson gson;

    public static class ResumenPartidaDTO {
        private String partidaId;
        private List<String> jugadores;
        private String estado;
        private int ronda;
        private int revision;

        public ResumenPartidaDTO(String partidaId, List<String> jugadores, String estado, int ronda, int revision) {
            this.partidaId = partidaId;
            this.jugadores = jugadores;
            this.estado = estado;
            this.ronda = ronda;
            this.revision = revision;
        }

        public String partidaId() { return partidaId; }
        public List<String> jugadores() { return jugadores; }
        public String estado() { return estado; }
        public int ronda() { return ronda; }
        public int revision() { return revision; }
    }

    public static class EstadisticasJugadorDTO {
        private String jugadorId;
        private String nombre;
        private String alias;
        private int partidasFinalizadas;
        private int victorias;
        private int puntosAcumulados;
        private int reliquiasEntregadas;

        public EstadisticasJugadorDTO(String jugadorId, String nombre, String alias,
                                      int partidasFinalizadas, int victorias,
                                      int puntosAcumulados, int reliquiasEntregadas) {
            this.jugadorId = jugadorId;
            this.nombre = nombre;
            this.alias = alias;
            this.partidasFinalizadas = partidasFinalizadas;
            this.victorias = victorias;
            this.puntosAcumulados = puntosAcumulados;
            this.reliquiasEntregadas = reliquiasEntregadas;
        }

        public String jugadorId() { return jugadorId; }
        public String nombre() { return nombre; }
        public String alias() { return alias; }
        public int partidasFinalizadas() { return partidasFinalizadas; }
        public int victorias() { return victorias; }
        public int puntosAcumulados() { return puntosAcumulados; }
        public int reliquiasEntregadas() { return reliquiasEntregadas; }
    }

    public ClienteApi(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    public boolean verificarSaludContratoV2() {
        try {
            JsonObject json = enviarGet("/salud").getAsJsonObject();
            boolean disp = json.has("disponible") && json.get("disponible").getAsBoolean();
            int ver = json.has("versionContrato") ? json.get("versionContrato").getAsInt() : 0;
            return disp && ver == 2;
        } catch (Exception e) {
            return false;
        }
    }

    public Jugador registrarJugador(String nombre, String alias) throws ApiComunicacionException {
        JsonObject body = new JsonObject();
        body.addProperty("nombre", nombre);
        body.addProperty("alias", alias);

        JsonObject resp = enviarPost("/jugadores", body).getAsJsonObject();
        return new Jugador(
                resp.get("jugadorId").getAsString(),
                resp.get("nombre").getAsString(),
                resp.get("alias").getAsString()
        );
    }

    public List<Jugador> listarJugadores(String filtroAlias) throws ApiComunicacionException {
        String ruta = "/jugadores";
        if (filtroAlias != null && !filtroAlias.isBlank()) {
            ruta += "?alias=" + URLEncoder.encode(filtroAlias.trim(), StandardCharsets.UTF_8);
        }
        JsonArray arr = enviarGet(ruta).getAsJsonArray();
        List<Jugador> lista = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            lista.add(new Jugador(
                    obj.get("jugadorId").getAsString(),
                    obj.get("nombre").getAsString(),
                    obj.get("alias").getAsString()
            ));
        }
        return lista;
    }

    public EstadisticasJugadorDTO consultarEstadisticas(String jugadorId) throws ApiComunicacionException {
        String idEnc = URLEncoder.encode(jugadorId.trim(), StandardCharsets.UTF_8);
        JsonObject obj = enviarGet("/jugadores/" + idEnc + "/estadisticas").getAsJsonObject();
        return new EstadisticasJugadorDTO(
                obj.get("jugadorId").getAsString(),
                obj.get("nombre").getAsString(),
                obj.get("alias").getAsString(),
                obj.get("partidasFinalizadas").getAsInt(),
                obj.get("victorias").getAsInt(),
                obj.get("puntosAcumulados").getAsInt(),
                obj.get("reliquiasEntregadas").getAsInt()
        );
    }

    public Partida crearPartida(String requestId, String jugador1Id, String jugador2Id) throws ApiComunicacionException {
        JsonObject body = new JsonObject();
        body.addProperty("requestId", requestId);
        JsonArray arr = new JsonArray();
        arr.add(jugador1Id);
        arr.add(jugador2Id);
        body.add("jugadores", arr);

        JsonObject resp = enviarPost("/partidas", body).getAsJsonObject();
        return reconstruirPartida(resp, obtenerMapaJugadores());
    }

    public List<ResumenPartidaDTO> listarPartidas(String filtroJugadorId) throws ApiComunicacionException {
        String ruta = "/partidas";
        if (filtroJugadorId != null && !filtroJugadorId.isBlank()) {
            ruta += "?jugadorId=" + URLEncoder.encode(filtroJugadorId.trim(), StandardCharsets.UTF_8);
        }
        JsonArray arr = enviarGet(ruta).getAsJsonArray();
        List<ResumenPartidaDTO> lista = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            List<String> jIds = new ArrayList<>();
            for (JsonElement jEl : obj.getAsJsonArray("jugadores")) {
                jIds.add(jEl.getAsString());
            }
            lista.add(new ResumenPartidaDTO(
                    obj.get("partidaId").getAsString(),
                    jIds,
                    obj.get("estado").getAsString(),
                    obj.get("ronda").getAsInt(),
                    obj.get("revision").getAsInt()
            ));
        }
        return lista;
    }

    public Partida cargarPartida(String partidaId) throws ApiComunicacionException {
        String idEnc = URLEncoder.encode(partidaId.trim(), StandardCharsets.UTF_8);
        JsonObject resp = enviarGet("/partidas/" + idEnc).getAsJsonObject();
        return reconstruirPartida(resp, obtenerMapaJugadores());
    }

    public ExplorarTemploAccion.DatosTemplo consultarContenidoTemplo(String partidaId, String coordenada)
            throws ApiComunicacionException {
        String idEnc = URLEncoder.encode(partidaId.trim(), StandardCharsets.UTF_8);
        String coordEnc = URLEncoder.encode(coordenada.trim().toUpperCase(), StandardCharsets.UTF_8);
        JsonObject obj = enviarGet("/partidas/" + idEnc + "/templos/" + coordEnc).getAsJsonObject();

        JsonObject relObj = obj.getAsJsonObject("reliquia");
        Reliquia reliquia = new Reliquia(relObj.get("reliquiaId").getAsString(), relObj.get("valor").getAsInt());

        Pista pista = null;
        if (obj.has("pista") && !obj.get("pista").isJsonNull()) {
            JsonObject pObj = obj.getAsJsonObject("pista");
            pista = new Pista(pObj.get("indice").getAsInt(), pObj.get("fragmento").getAsString());
        }
        return new ExplorarTemploAccion.DatosTemplo(reliquia, pista);
    }

    public PaqueteCifradoCofre obtenerPaqueteCofre(String partidaId) throws ApiComunicacionException {
        String idEnc = URLEncoder.encode(partidaId.trim(), StandardCharsets.UTF_8);
        JsonObject obj = enviarGet("/partidas/" + idEnc + "/cofre").getAsJsonObject();
        return gson.fromJson(obj, PaqueteCifradoCofre.class);
    }

    public int guardarEstadoPartida(Partida partida) throws ApiComunicacionException {
        String idEnc = URLEncoder.encode(partida.getPartidaId(), StandardCharsets.UTF_8);

        JsonObject payload = new JsonObject();
        payload.addProperty("revisionBase", partida.getRevisionActual());
        payload.add("estado", armarEstadoJson(partida));

        JsonObject resp = enviarPut("/partidas/" + idEnc + "/estado", payload).getAsJsonObject();
        int nuevaRev = resp.get("revision").getAsInt();
        partida.setRevisionActual(nuevaRev);
        return nuevaRev;
    }

    private Map<String, Jugador> obtenerMapaJugadores() throws ApiComunicacionException {
        List<Jugador> lista = listarJugadores(null);
        Map<String, Jugador> mapa = new HashMap<>();
        for (Jugador j : lista) {
            mapa.put(j.getJugadorId(), j);
        }
        return mapa;
    }

    private Partida reconstruirPartida(JsonObject json, Map<String, Jugador> catalogo) {
        String partidaId = json.get("partidaId").getAsString();
        String campamento = json.get("campamento").getAsString();
        String cofrePos = json.get("cofrePosicion").getAsString();
        String cofreId = json.get("cofreId").getAsString();
        int revision = json.get("revision").getAsInt();

        JsonArray tabArr = json.getAsJsonArray("tablero");
        List<Casilla> casillas = new ArrayList<>();
        int maxFila = 0;
        int maxCol = 0;
        for (JsonElement el : tabArr) {
            JsonObject cObj = el.getAsJsonObject();
            String coord = cObj.get("coordenada").getAsString().toUpperCase();
            String tipoStr = cObj.get("tipo").getAsString();
            int col = coord.charAt(0) - 'A';
            int fila = Integer.parseInt(coord.substring(1)) - 1;
            if (fila + 1 > maxFila) maxFila = fila + 1;
            if (col + 1 > maxCol) maxCol = col + 1;
            casillas.add(new Casilla(fila, col, coord, TipoTerreno.desdeSimbolo(tipoStr)));
        }
        Tablero tablero = new Tablero(maxFila, maxCol, casillas);

        JsonObject estObj = json.getAsJsonObject("estado");
        JsonArray jugArr = estObj.getAsJsonArray("jugadores");
        List<Participante> participantes = new ArrayList<>();

        for (JsonElement el : jugArr) {
            JsonObject jEst = el.getAsJsonObject();
            String jId = jEst.get("jugadorId").getAsString();
            Jugador base = catalogo.getOrDefault(jId, new Jugador(jId, jId, jId));

            String pos = jEst.get("posicion").getAsString();
            int puntos = jEst.get("puntos").getAsInt();
            long tiempo = jEst.has("tiempoConsumidoSegundos") ? jEst.get("tiempoConsumidoSegundos").getAsLong() : 0L;

            List<Reliquia> inv = leerReliquias(jEst.getAsJsonArray("inventario"));
            List<Reliquia> ent = leerReliquias(jEst.getAsJsonArray("entregadas"));
            participantes.add(new Participante(base, pos, puntos, inv, ent, tiempo));
        }

        Partida partida = new Partida(partidaId, cofreId, campamento, cofrePos, tablero, participantes, revision);

        EstadoPartida estadoEnum = EstadoPartida.valueOf(estObj.get("estado").getAsString());
        int ronda = estObj.get("ronda").getAsInt();
        int idxActual = estObj.get("indiceJugadorActual").getAsInt();
        int accRestantes = estObj.get("accionesRestantes").getAsInt();
        boolean cofreAbierto = estObj.get("cofreAbierto").getAsBoolean();

        List<String> explorados = new ArrayList<>();
        for (JsonElement el : estObj.getAsJsonArray("templosExplorados")) {
            explorados.add(el.getAsString());
        }

        List<Pista> pistas = new ArrayList<>();
        for (JsonElement el : estObj.getAsJsonArray("pistas")) {
            JsonObject pObj = el.getAsJsonObject();
            pistas.add(new Pista(pObj.get("indice").getAsInt(), pObj.get("fragmento").getAsString()));
        }

        List<String> ganadores = new ArrayList<>();
        if (estObj.has("ganadores") && estObj.get("ganadores").isJsonArray()) {
            for (JsonElement el : estObj.getAsJsonArray("ganadores")) {
                ganadores.add(el.getAsString());
            }
        }

        partida.restaurarEstadoSnapshot(estadoEnum, ronda, idxActual, accRestantes, explorados, pistas, cofreAbierto, ganadores, revision);
        return partida;
    }

    private List<Reliquia> leerReliquias(JsonArray arr) {
        List<Reliquia> lista = new ArrayList<>();
        if (arr == null) return lista;
        for (JsonElement el : arr) {
            JsonObject rObj = el.getAsJsonObject();
            lista.add(new Reliquia(rObj.get("reliquiaId").getAsString(), rObj.get("valor").getAsInt()));
        }
        return lista;
    }

    private JsonObject armarEstadoJson(Partida partida) {
        JsonObject est = new JsonObject();
        est.addProperty("estado", partida.getEstado().name());
        est.addProperty("ronda", partida.getRonda());
        est.addProperty("indiceJugadorActual", partida.getIndiceJugadorActual());
        est.addProperty("accionesRestantes", partida.getAccionesRestantes());

        JsonArray jugArr = new JsonArray();
        for (Participante p : partida.getParticipantes()) {
            JsonObject jObj = new JsonObject();
            jObj.addProperty("jugadorId", p.getJugadorId());
            jObj.addProperty("posicion", p.getPosicion());
            jObj.addProperty("puntos", p.getPuntos());
            jObj.add("inventario", convertirReliquias(p.getReliquiasInventario()));
            jObj.add("entregadas", convertirReliquias(p.getReliquiasEntregadas()));
            jObj.addProperty("tiempoConsumidoSegundos", p.getTiempoConsumidoSegundos());
            jugArr.add(jObj);
        }
        est.add("jugadores", jugArr);

        JsonArray expArr = new JsonArray();
        for (String c : partida.getTemplosExplorados()) {
            expArr.add(c);
        }
        est.add("templosExplorados", expArr);

        JsonArray pistasArr = new JsonArray();
        for (Pista pista : partida.getPistasDescubiertas()) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("indice", pista.getIndice());
            pObj.addProperty("fragmento", pista.getFragmento());
            pistasArr.add(pObj);
        }
        est.add("pistas", pistasArr);
        est.addProperty("cofreAbierto", partida.isCofreAbierto());

        JsonArray ganArr = new JsonArray();
        for (String g : partida.getGanadores()) {
            ganArr.add(g);
        }
        est.add("ganadores", ganArr);
        return est;
    }

    private JsonArray convertirReliquias(List<Reliquia> reliquias) {
        JsonArray arr = new JsonArray();
        for (Reliquia r : reliquias) {
            JsonObject obj = new JsonObject();
            obj.addProperty("reliquiaId", r.getReliquiaId());
            obj.addProperty("valor", r.getValor());
            arr.add(obj);
        }
        return arr;
    }

    private JsonElement enviarGet(String ruta) throws ApiComunicacionException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + ruta))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        return ejecutarPeticion(req);
    }

    private JsonElement enviarPost(String ruta, JsonObject body) throws ApiComunicacionException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + ruta))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();
        return ejecutarPeticion(req);
    }

    private JsonElement enviarPut(String ruta, JsonObject body) throws ApiComunicacionException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + ruta))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();
        return ejecutarPeticion(req);
    }

    private JsonElement ejecutarPeticion(HttpRequest request) throws ApiComunicacionException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String body = response.body();

            if (status >= 200 && status < 300) {
                return JsonParser.parseString(body);
            }

            String codigo = "ERROR_HTTP_" + status;
            String mensaje = "Error HTTP " + status;
            try {
                JsonObject err = JsonParser.parseString(body).getAsJsonObject();
                if (err.has("codigo")) codigo = err.get("codigo").getAsString();
                if (err.has("mensaje")) mensaje = err.get("mensaje").getAsString();
            } catch (Exception ignored) {
            }
            throw new ApiComunicacionException(status, codigo, mensaje);
        } catch (IOException | InterruptedException e) {
            throw new ApiComunicacionException("No se pudo conectar con el servidor en " + baseUrl, e);
        }
    }
}