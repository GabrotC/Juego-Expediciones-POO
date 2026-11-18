package ve.edu.ucab.poo.modelo;

import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import java.util.List;

public class Tablero {
    private int filas;
    private int columnas;
    private Casilla[][] matriz;

    public Tablero(int filas, int columnas, List<Casilla> casillas) {
        this.filas = filas;
        this.columnas = columnas;
        this.matriz = new Casilla[filas][columnas];
        for (Casilla c : casillas) {
            this.matriz[c.getFila()][c.getColumna()] = c;
        }
    }

    public int getFilas() {
        return filas;
    }

    public int getColumnas() {
        return columnas;
    }

    public Casilla obtenerCasilla(String coordenada) throws ReglaJuegoException {
        if (coordenada == null || coordenada.trim().length() < 2) {
            throw new ReglaJuegoException("Coordenada invalida.");
        }
        String limpia = coordenada.trim().toUpperCase();
        int col = limpia.charAt(0) - 'A';
        int fila;
        try {
            fila = Integer.parseInt(limpia.substring(1)) - 1;
        } catch (NumberFormatException e) {
            throw new ReglaJuegoException("Coordenada fuera del mapa: " + coordenada);
        }

        if (fila < 0 || fila >= filas || col < 0 || col >= columnas) {
            throw new ReglaJuegoException("Coordenada fuera del mapa: " + coordenada);
        }
        return matriz[fila][col];
    }

    public Casilla obtenerCasillaPorIndice(int fila, int col) {
        return matriz[fila][col];
    }

    public boolean sonVecinosOrtogonales(String origenCoord, String destinoCoord) throws ReglaJuegoException {
        Casilla origen = obtenerCasilla(origenCoord);
        Casilla destino = obtenerCasilla(destinoCoord);
        int dist = Math.abs(origen.getFila() - destino.getFila()) + Math.abs(origen.getColumna() - destino.getColumna());
        return dist == 1;
    }
}