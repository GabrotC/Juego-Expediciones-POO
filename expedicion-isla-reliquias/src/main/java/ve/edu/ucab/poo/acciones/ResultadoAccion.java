package ve.edu.ucab.poo.acciones;

public class ResultadoAccion {
    private boolean exito;
    private int puntosConsumidos;
    private String mensaje;

    public ResultadoAccion(boolean exito, int puntosConsumidos, String mensaje) {
        this.exito = exito;
        this.puntosConsumidos = puntosConsumidos;
        this.mensaje = mensaje;
    }

    public boolean isExito() {
        return exito;
    }

    public int getPuntosConsumidos() {
        return puntosConsumidos;
    }

    public String getMensaje() {
        return mensaje;
    }
}