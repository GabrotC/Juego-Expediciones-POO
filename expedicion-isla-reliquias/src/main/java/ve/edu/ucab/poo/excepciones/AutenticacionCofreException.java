package ve.edu.ucab.poo.excepciones;

public class AutenticacionCofreException extends Exception {
    public AutenticacionCofreException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}