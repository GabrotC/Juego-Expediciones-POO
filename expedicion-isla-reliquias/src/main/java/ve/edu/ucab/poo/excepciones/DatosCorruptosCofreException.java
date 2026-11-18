package ve.edu.ucab.poo.excepciones;

public class DatosCorruptosCofreException extends Exception {
    public DatosCorruptosCofreException(String mensaje) {
        super(mensaje);
    }

    public DatosCorruptosCofreException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}