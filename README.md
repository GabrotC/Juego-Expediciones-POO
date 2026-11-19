# Expedición: La Isla de las Reliquias

Proyecto de Programación Orientada a Objetos (POO) dividido en dos módulos que trabajan juntos: un servidor API REST en **Python (FastAPI)** y un cliente interactivo de consola en **Java 22 (Maven)**.

## Requisitos previos

- Tener instalado **Python 3.12+** (con `pip` y `venv`).
- Tener instalado **Java 22 (JDK)** y **Apache Maven** (o **IntelliJ IDEA** configurado con JDK 22 y Maven).

## 1. Levantar el backend en Python (hacerlo primero)

1. Abre una terminal (PowerShell o CMD) en la carpeta raíz del repositorio clonado.
2. Entra a la carpeta del backend, crea el entorno virtual y actívalo:

```powershell
cd backend-python
python -m venv .venv
.\.venv\Scripts\activate
```

3. Instala las dependencias necesarias desde `requirements.txt`:

```powershell
pip install -r requirements.txt
```

4. Inicia el servidor de la API en el puerto `8000` (déjalo abierto mientras uses el juego):

```powershell
uvicorn main:app --reload --port 8000
```

5. Para probar o revisar los endpoints:
   - Documentación interactiva (Swagger): abre `http://localhost:8000/docs` en el navegador.
   - Colección de Postman: importa el archivo `backend-python/postman_collection.json` y asegúrate de usar la variable `base_url` en `http://localhost:8000/api/v1`.
   - Ya vienen datos de prueba precargados (como los jugadores `J1` y `J2` y la partida `EX-RED-01`).

## 2. Ejecutar el cliente en Java 22

Con el servidor de Python ya corriendo en la primera terminal, abre **otra** terminal en la carpeta raíz del repositorio:

### Opción A: Desde consola con Maven

1. Entra a la carpeta del proyecto Java:

```powershell
cd expedicion-isla-reliquias
```

2. Ejecuta las pruebas unitarias para verificar que todo compila y pasa:

```powershell
mvn test
```

3. Inicia el juego por consola:

```powershell
mvn exec:java -Dexec.mainClass="ve.edu.ucab.poo.Main"
```

### Opción B: Desde IntelliJ IDEA

1. Abre **IntelliJ IDEA**, selecciona **File > Open** y elige la carpeta `expedicion-isla-reliquias` (donde está el `pom.xml`).
2. Espera a que Maven termine de descargar y sincronizar las dependencias (`Gson` y `JUnit`).
3. Busca y ejecuta la clase `Main.java` (ubicada en `src/main/java/ve/edu/ucab/poo/Main.java`).
