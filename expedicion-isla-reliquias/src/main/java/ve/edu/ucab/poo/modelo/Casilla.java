package ve.edu.ucab.poo.modelo;

public class Casilla {
    private int fila;
    private int columna;
    private String coordenada;
    private TipoTerreno terreno;

    public Casilla(int fila, int columna, String coordenada, TipoTerreno terreno) {
        this.fila = fila;
        this.columna = columna;
        this.coordenada = coordenada.toUpperCase();
        this.terreno = terreno;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public String getCoordenada() {
        return coordenada;
    }

    public TipoTerreno getTerreno() {
        return terreno;
    }

    public int getCostoEntrada() {
        return terreno.getCostoEntrada();
    }
}