import os
import re
import json
import random
import base64
import hashlib
import tempfile
from collections import deque
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

app = FastAPI(title="API Expedicion Isla Reliquias", version="2.0.0")

DATA_DIR = "data"
DB_FILE = os.path.join(DATA_DIR, "expedicion_db.json")

COLUMNAS = ['A', 'B', 'C', 'D', 'E']
FILAS = ['1', '2', '3', '4', '5']
PALABRAS = ["ORBITA", "TEMPLO", "ISLAZO", "TESORO", "SELVAJ", "BRUJUL", "TOTEMS", "RELIQU", "CUMBRE", "FAROES"]

REGEX_NOMBRE = re.compile(r"^.{1,80}$")
REGEX_ALIAS = re.compile(r"^[A-Za-z0-9_-]{3,20}$")
REGEX_COORD = re.compile(r"^[A-E][1-5]$")


class CryptoService:
    @staticmethod
    def derivar_clave(password: str, salt: bytes) -> bytes:
        return hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000, 32)

    @staticmethod
    def cifrar_cofre(partida_id: str, cofre_id: str, reliquia_id: str, valor: int, password: str) -> dict:
        salt = os.urandom(16)
        nonce = os.urandom(12)
        key = CryptoService.derivar_clave(password, salt)

        datos = {"partidaId": partida_id, "cofreId": cofre_id, "reliquiaId": reliquia_id, "valor": valor}
        payload = json.dumps(datos, separators=(',', ':')).encode('utf-8')
        aad = f"{partida_id}|{cofre_id}".encode('utf-8')

        aesgcm = AESGCM(key)
        cifrado = aesgcm.encrypt(nonce, payload, aad)

        return {
            "partidaId": partida_id,
            "cofreId": cofre_id,
            "algoritmo": "AES/GCM/NoPadding",
            "kdf": "PBKDF2WithHmacSHA256",
            "iteraciones": 100000,
            "longitudClaveBits": 256,
            "saltBase64": base64.b64encode(salt).decode('utf-8'),
            "nonceBase64": base64.b64encode(nonce).decode('utf-8'),
            "tagBits": 128,
            "aadBase64": base64.b64encode(aad).decode('utf-8'),
            "cifradoConTagBase64": base64.b64encode(cifrado).decode('utf-8')
        }


def a_coordenada(f: int, c: int) -> str:
    return f"{COLUMNAS[c]}{FILAS[f]}"


def obtener_vecinos(f: int, c: int):
    res = []
    for df, dc in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        nf, nc = f + df, c + dc
        if 0 <= nf < 5 and 0 <= nc < 5:
            res.append((nf, nc))
    return res


def calcular_distancia(matriz, inicio, fin) -> int:
    if inicio == fin:
        return 0
    visitados = {inicio}
    cola = deque([(inicio[0], inicio[1], 0)])
    while cola:
        f, c, d = cola.popleft()
        for nf, nc in obtener_vecinos(f, c):
            if (nf, nc) not in visitados and matriz[nf][nc] != 'P':
                if (nf, nc) == fin:
                    return d + 1
                visitados.add((nf, nc))
                cola.append((nf, nc, d + 1))
    return 9999


def generar_escenario(partida_id: str, request_id: str, jugadores: list, config_previa: dict = None) -> dict:
    base = ['C', 'X'] + ['T'] * 4 + ['P'] * 4 + ['S'] * 15
    coords = [(f, c) for f in range(5) for c in range(5)]

    prev_c = config_previa.get("campamento") if config_previa else None
    prev_x = config_previa.get("cofre_pos") if config_previa else None
    prev_p1 = config_previa.get("pista1") if config_previa else None
    prev_p2 = config_previa.get("pista2") if config_previa else None

    for _ in range(1000):
        tipos = base[:]
        random.shuffle(tipos)
        matriz = [[tipos[f * 5 + c] for c in range(5)] for f in range(5)]

        pos_c = next((f, c) for f, c in coords if matriz[f][c] == 'C')
        pos_x = next((f, c) for f, c in coords if matriz[f][c] == 'X')
        templos = [(f, c) for f, c in coords if matriz[f][c] == 'T']

        coord_c = a_coordenada(*pos_c)
        coord_x = a_coordenada(*pos_x)

        if (prev_c and coord_c == prev_c) or (prev_x and coord_x == prev_x):
            continue

        vecinos_libres = [v for v in obtener_vecinos(*pos_c) if matriz[v[0]][v[1]] != 'P']
        if len(vecinos_libres) < 2:
            continue

        dist_min_templo = min(calcular_distancia(matriz, pos_c, t) for t in templos)
        if dist_min_templo > 2:
            continue

        t1, t2 = random.sample(templos, 2)
        d_cx = calcular_distancia(matriz, pos_c, pos_x)
        d_t1_t2 = calcular_distancia(matriz, t1, t2)

        ruta1 = calcular_distancia(matriz, pos_c, t1) + d_t1_t2 + calcular_distancia(matriz, t2, pos_c) + 2 * d_cx
        ruta2 = calcular_distancia(matriz, pos_c, t2) + d_t1_t2 + calcular_distancia(matriz, t1, pos_c) + 2 * d_cx

        if min(ruta1, ruta2) > 12:
            continue

        opciones_p1 = [p for p in PALABRAS if p != prev_p1]
        pista1 = random.choice(opciones_p1)
        pista2 = f"{random.randint(0, 9999):04d}"
        while pista2 == prev_p2:
            pista2 = f"{random.randint(0, 9999):04d}"

        clave = f"{pista1}-{pista2}"
        paquete = CryptoService.cifrar_cofre(partida_id, "COFRE-01", "REL-ESPECIAL", 4, clave)

        valores = [1, 2, 2, 3]
        random.shuffle(valores)
        templos_info = {}

        for i, pos_t in enumerate(templos):
            c_str = a_coordenada(*pos_t)
            pista_data = None
            if pos_t == t1:
                pista_data = {"indice": 1, "fragmento": pista1}
            elif pos_t == t2:
                pista_data = {"indice": 2, "fragmento": pista2}

            templos_info[c_str] = {
                "coordenada": c_str,
                "reliquia": {"reliquiaId": f"REL-0{i + 1}", "valor": valores[i]},
                "pista": pista_data
            }

        tablero_lista = []
        for f in range(5):
            for c in range(5):
                tablero_lista.append({"coordenada": a_coordenada(f, c), "tipo": matriz[f][c]})

        orden = jugadores[:]
        random.shuffle(orden)

        estado_ini = {
            "estado": "EN_CURSO",
            "ronda": 1,
            "indiceJugadorActual": 0,
            "accionesRestantes": 3,
            "jugadores": [
                {
                    "jugadorId": j,
                    "posicion": coord_c,
                    "puntos": 0,
                    "inventario": [],
                    "entregadas": [],
                    "tiempoConsumidoSegundos": 0
                } for j in orden
            ],
            "templosExplorados": [],
            "pistas": [],
            "cofreAbierto": False,
            "ganadores": []
        }

        return {
            "partidaId": partida_id,
            "requestId": request_id,
            "jugadores": orden,
            "campamento": coord_c,
            "cofre": {
                "cofreId": "COFRE-01",
                "posicion": coord_x,
                "paqueteCifrado": paquete,
                "abierto": False
            },
            "tablero": tablero_lista,
            "templosContenido": templos_info,
            "revisionActual": 1,
            "estadoActual": estado_ini,
            "ultimoSnapshotGuardado": None,
            "ultimaBaseUsada": None,
            "_pista1": pista1,
            "_pista2": pista2
        }

    raise RuntimeError("No se pudo generar un tablero valido")


class BaseDatos:
    def __init__(self):
        os.makedirs(DATA_DIR, exist_ok=True)
        self.jugadores = {}
        self.partidas = {}
        self.requests = {}
        self.ultimas_configs = {}
        self.cargar()
        self.cargar_datos_prueba()

    def cargar(self):
        if not os.path.exists(DB_FILE):
            return
        try:
            with open(DB_FILE, "r", encoding="utf-8") as f:
                d = json.load(f)
                self.jugadores = d.get("jugadores", {})
                self.partidas = d.get("partidas", {})
                self.requests = d.get("requests_creacion", {})
                self.ultimas_configs = d.get("ultimas_configs_par", {})
        except Exception:
            pass

    def guardar(self):
        os.makedirs(DATA_DIR, exist_ok=True)
        d = {
            "jugadores": self.jugadores,
            "partidas": self.partidas,
            "requests_creacion": self.requests,
            "ultimas_configs_par": self.ultimas_configs
        }
        fd, temp_path = tempfile.mkstemp(dir=DATA_DIR, suffix=".tmp")
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                json.dump(d, f, ensure_ascii=False, indent=2)
            os.replace(temp_path, DB_FILE)
        except Exception as e:
            if os.path.exists(temp_path):
                os.remove(temp_path)
            raise e

    def cargar_datos_prueba(self):
        if "J1" not in self.jugadores:
            self.jugadores["J1"] = {"jugadorId": "J1", "nombre": "Ana Pérez", "alias": "ana_ucab"}
        if "J2" not in self.jugadores:
            self.jugadores["J2"] = {"jugadorId": "J2", "nombre": "Luis Gómez", "alias": "luis_ucab"}

        if "EX-RED-01" not in self.partidas:
            mapa = {
                "A1": "S", "B1": "T", "C1": "S", "D1": "P", "E1": "S",
                "A2": "P", "B2": "S", "C2": "S", "D2": "X", "E2": "S",
                "A3": "S", "B3": "T", "C3": "C", "D3": "S", "E3": "P",
                "A4": "S", "B4": "S", "C4": "T", "D4": "S", "E4": "S",
                "A5": "T", "B5": "P", "C5": "S", "D5": "S", "E5": "S"
            }
            paquete = {
                "partidaId": "EX-RED-01",
                "cofreId": "COFRE-01",
                "algoritmo": "AES/GCM/NoPadding",
                "kdf": "PBKDF2WithHmacSHA256",
                "iteraciones": 100000,
                "longitudClaveBits": 256,
                "saltBase64": "xBxRniX+mjbP3g16qy93zA==",
                "nonceBase64": "BVmBDNDCL0Gw5CM6",
                "tagBits": 128,
                "aadBase64": "RVgtUkVELTAxfENPRlJFLTAx",
                "cifradoConTagBase64": "u/hosFuUuUt+RKGBQV9S/amTmkZd5SwCLtnzyn6/EgT9Y3Nmhs7G9JfRfXMZPce426aLkJRpuGdof1jG0pikC+bkl1DmSNPrLIES17AuJ9UZaWvXl2pgbL84kWXKITU5noqdPQ=="
            }
            templos = {
                "B3": {"coordenada": "B3", "reliquia": {"reliquiaId": "REL-01", "valor": 3}, "pista": {"indice": 1, "fragmento": "ORBITA"}},
                "C4": {"coordenada": "C4", "reliquia": {"reliquiaId": "REL-02", "valor": 2}, "pista": {"indice": 2, "fragmento": "7392"}},
                "B1": {"coordenada": "B1", "reliquia": {"reliquiaId": "REL-03", "valor": 2}, "pista": None},
                "A5": {"coordenada": "A5", "reliquia": {"reliquiaId": "REL-04", "valor": 1}, "pista": None}
            }
            estado = {
                "estado": "EN_CURSO",
                "ronda": 1,
                "indiceJugadorActual": 0,
                "accionesRestantes": 3,
                "jugadores": [
                    {"jugadorId": "J1", "posicion": "C3", "puntos": 0, "inventario": [], "entregadas": [], "tiempoConsumidoSegundos": 0},
                    {"jugadorId": "J2", "posicion": "C3", "puntos": 0, "inventario": [], "entregadas": [], "tiempoConsumidoSegundos": 0}
                ],
                "templosExplorados": [],
                "pistas": [],
                "cofreAbierto": False,
                "ganadores": []
            }
            self.partidas["EX-RED-01"] = {
                "partidaId": "EX-RED-01",
                "requestId": "solicitud-anexo-b",
                "jugadores": ["J1", "J2"],
                "campamento": "C3",
                "cofre": {"cofreId": "COFRE-01", "posicion": "D2", "paqueteCifrado": paquete, "abierto": False},
                "tablero": [{"coordenada": k, "tipo": v} for k, v in mapa.items()],
                "templosContenido": templos,
                "revisionActual": 1,
                "estadoActual": estado,
                "ultimoSnapshotGuardado": None,
                "ultimaBaseUsada": None
            }
            self.guardar()


db = BaseDatos()


def lanzar_error(status: int, codigo: str, mensaje: str):
    raise HTTPException(status_code=status, detail={"codigo": codigo, "mensaje": mensaje})


@app.exception_handler(HTTPException)
async def http_err_handler(request: Request, exc: HTTPException):
    if isinstance(exc.detail, dict) and "codigo" in exc.detail:
        return JSONResponse(status_code=exc.status_code, content=exc.detail)
    return JSONResponse(status_code=exc.status_code, content={"codigo": "ERROR_HTTP", "mensaje": str(exc.detail)})


@app.exception_handler(RequestValidationError)
async def val_err_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(status_code=400, content={"codigo": "SOLICITUD_INVALIDA", "mensaje": "Datos invalidos"})


async def leer_json(request: Request) -> dict:
    try:
        raw = await request.body()
        if not raw:
            lanzar_error(400, "JSON_VACIO", "Cuerpo vacio")
        data = json.loads(raw.decode("utf-8"))
        if not isinstance(data, dict):
            lanzar_error(400, "JSON_INVALIDO", "Formato invalido")
        return data
    except HTTPException:
        raise
    except Exception:
        lanzar_error(400, "JSON_INVALIDO", "No se pudo leer el JSON")


def formato_partida(p: dict) -> dict:
    return {
        "partidaId": p["partidaId"],
        "jugadores": p["jugadores"],
        "campamento": p["campamento"],
        "cofrePosicion": p["cofre"]["posicion"],
        "cofreId": p["cofre"]["cofreId"],
        "tablero": p["tablero"],
        "revision": p["revisionActual"],
        "estado": p["estadoActual"]
    }


@app.get("/api/v1/salud")
@app.get("/api/v2/salud")
def salud():
    return {"disponible": True, "versionContrato": 2}


@app.post("/api/v1/jugadores", status_code=201)
async def crear_jugador(request: Request):
    body = await leer_json(request)
    nombre = str(body.get("nombre", "")).strip()
    alias = str(body.get("alias", "")).strip()

    if not REGEX_NOMBRE.match(nombre):
        lanzar_error(400, "NOMBRE_INVALIDO", "Nombre invalido")
    if not REGEX_ALIAS.match(alias):
        lanzar_error(400, "ALIAS_INVALIDO", "Alias invalido")

    for j in db.jugadores.values():
        if j["alias"].lower() == alias.lower():
            lanzar_error(409, "ALIAS_DUPLICADO", "El alias ya existe")

    n = len(db.jugadores) + 1
    nuevo_id = f"J{n}"
    while nuevo_id in db.jugadores:
        n += 1
        nuevo_id = f"J{n}"

    jugador = {"jugadorId": nuevo_id, "nombre": nombre, "alias": alias}
    db.jugadores[nuevo_id] = jugador
    db.guardar()
    return jugador


@app.get("/api/v1/jugadores")
def obtener_jugadores(alias: str = Query(None)):
    lista = list(db.jugadores.values())
    if alias is not None:
        busq = alias.strip().lower()
        lista = [j for j in lista if busq == j["alias"].lower() or busq in j["alias"].lower()]
    return lista


@app.get("/api/v1/jugadores/{jugador_id}/estadisticas")
def estadisticas_jugador(jugador_id: str):
    if jugador_id not in db.jugadores:
        lanzar_error(404, "JUGADOR_NO_ENCONTRADO", "Jugador no existe")

    finalizadas = 0
    victorias = 0
    puntos = 0
    entregadas = 0

    for p in db.partidas.values():
        est = p["estadoActual"]
        if est.get("estado") == "FINALIZADA" and jugador_id in p["jugadores"]:
            finalizadas += 1
            if jugador_id in est.get("ganadores", []):
                victorias += 1
            for j in est.get("jugadores", []):
                if j.get("jugadorId") == jugador_id:
                    puntos += int(j.get("puntos", 0))
                    entregadas += len(j.get("entregadas", []))

    return {
        "jugadorId": jugador_id,
        "nombre": db.jugadores[jugador_id]["nombre"],
        "alias": db.jugadores[jugador_id]["alias"],
        "partidasFinalizadas": finalizadas,
        "victorias": victorias,
        "puntosAcumulados": puntos,
        "reliquiasEntregadas": entregadas
    }


@app.post("/api/v1/partidas", status_code=201)
async def nueva_partida(request: Request):
    body = await leer_json(request)
    req_id = str(body.get("requestId", "")).strip()
    jugadores = body.get("jugadores")

    if not req_id:
        lanzar_error(400, "REQUEST_ID_INVALIDO", "Falta requestId")

    if req_id in db.requests:
        prev = db.requests[req_id]
        if prev["jugadores"] == jugadores:
            return JSONResponse(status_code=200, content=formato_partida(db.partidas[prev["partidaId"]]))
        lanzar_error(409, "REQUEST_ID_CONFLICTO", "requestId usado con otros jugadores")

    if not isinstance(jugadores, list) or len(jugadores) != 2:
        lanzar_error(400, "JUGADORES_INVALIDOS", "Se requieren 2 jugadores")
    if jugadores[0] == jugadores[1]:
        lanzar_error(400, "JUGADORES_DUPLICADOS", "Jugadores repetidos")
    for j in jugadores:
        if j not in db.jugadores:
            lanzar_error(404, "JUGADOR_NO_ENCONTRADO", f"No existe {j}")

    par = "|".join(sorted(jugadores))
    cfg_previa = db.ultimas_configs.get(par)

    n = len(db.partidas) + 1
    nuevo_id = f"EX-RED-{n:02d}"
    while nuevo_id in db.partidas:
        n += 1
        nuevo_id = f"EX-RED-{n:02d}"

    try:
        partida = generar_escenario(nuevo_id, req_id, jugadores, cfg_previa)
    except RuntimeError as e:
        lanzar_error(503, "GENERACION_FALLIDA", str(e))

    db.ultimas_configs[par] = {
        "campamento": partida["campamento"],
        "cofre_pos": partida["cofre"]["posicion"],
        "pista1": partida.pop("_pista1"),
        "pista2": partida.pop("_pista2")
    }

    db.partidas[nuevo_id] = partida
    db.requests[req_id] = {"jugadores": jugadores, "partidaId": nuevo_id}
    db.guardar()
    return formato_partida(partida)


@app.get("/api/v1/partidas")
def listar_partidas(jugadorId: str = Query(None)):
    res = []
    for p in db.partidas.values():
        if jugadorId and jugadorId not in p["jugadores"]:
            continue
        est = p["estadoActual"]
        res.append({
            "partidaId": p["partidaId"],
            "jugadores": p["jugadores"],
            "estado": est.get("estado", "EN_CURSO"),
            "ronda": est.get("ronda", 1),
            "revision": p["revisionActual"]
        })
    return res


@app.get("/api/v1/partidas/{partida_id}")
def ver_partida(partida_id: str):
    if partida_id not in db.partidas:
        lanzar_error(404, "PARTIDA_NO_ENCONTRADA", "No existe la partida")
    return formato_partida(db.partidas[partida_id])


@app.get("/api/v1/partidas/{partida_id}/templos/{coord}")
def ver_templo(partida_id: str, coord: str):
    if partida_id not in db.partidas:
        lanzar_error(404, "PARTIDA_NO_ENCONTRADA", "No existe la partida")
    c = coord.strip().upper()
    if not REGEX_COORD.match(c):
        lanzar_error(400, "COORDENADA_INVALIDA", "Coordenada fuera de rango")
    templos = db.partidas[partida_id]["templosContenido"]
    if c not in templos:
        lanzar_error(400, "CASILLA_NO_ES_TEMPLO", "La casilla no es un templo")
    return templos[c]


@app.get("/api/v1/partidas/{partida_id}/cofre")
def ver_cofre(partida_id: str):
    if partida_id not in db.partidas:
        lanzar_error(404, "PARTIDA_NO_ENCONTRADA", "No existe la partida")
    return db.partidas[partida_id]["cofre"]["paqueteCifrado"]


@app.put("/api/v1/partidas/{partida_id}/estado")
async def actualizar_estado(partida_id: str, request: Request):
    if partida_id not in db.partidas:
        lanzar_error(404, "PARTIDA_NO_ENCONTRADA", "No existe la partida")

    body = await leer_json(request)
    if "revisionBase" not in body or "estado" not in body:
        lanzar_error(400, "CAMPOS_FALTANTES", "Faltan datos")

    try:
        rev_base = int(body["revisionBase"])
    except (ValueError, TypeError):
        lanzar_error(400, "REVISION_INVALIDA", "revisionBase debe ser entero")

    nuevo_estado = body["estado"]
    p = db.partidas[partida_id]
    rev_actual = p["revisionActual"]

    req_keys = {"estado", "ronda", "indiceJugadorActual", "accionesRestantes", "jugadores", "templosExplorados", "pistas", "cofreAbierto", "ganadores"}
    if not isinstance(nuevo_estado, dict) or not req_keys.issubset(nuevo_estado.keys()):
        lanzar_error(400, "ESTADO_INVALIDO", "Estructura de estado incompleta")

    if p.get("ultimaBaseUsada") == rev_base and p.get("ultimoSnapshotGuardado") == nuevo_estado:
        return {"partidaId": partida_id, "revision": rev_actual, "guardado": True, "estado": p["estadoActual"]}

    if p["estadoActual"].get("estado") == "FINALIZADA":
        lanzar_error(409, "PARTIDA_YA_FINALIZADA", "La partida ya termino")

    if rev_base != rev_actual:
        lanzar_error(409, "REVISION_CONFLICTO", "Conflicto de revision")

    p["ultimaBaseUsada"] = rev_base
    p["ultimoSnapshotGuardado"] = nuevo_estado
    p["estadoActual"] = nuevo_estado
    p["revisionActual"] = rev_actual + 1
    p["cofre"]["abierto"] = bool(nuevo_estado.get("cofreAbierto", False))

    db.guardar()
    return {"partidaId": partida_id, "revision": p["revisionActual"], "guardado": True, "estado": p["estadoActual"]}