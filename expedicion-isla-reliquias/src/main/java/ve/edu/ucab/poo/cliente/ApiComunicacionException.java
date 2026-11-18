package ve.edu.ucab.poo.cliente;

public class ApiComunicacionException extends Exception {
    private int statusCode;
    private String codigoError;

    public ApiComunicacionException(int statusCode, String codigoError, String mensaje) {
        super(mensaje);
        this.statusCode = statusCode;
        this.codigoError = codigoError;
    }

    public ApiComunicacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.statusCode = 0;
        this.codigoError = "ERROR_RED";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCodigoError() {
        return codigoError;
    }
}