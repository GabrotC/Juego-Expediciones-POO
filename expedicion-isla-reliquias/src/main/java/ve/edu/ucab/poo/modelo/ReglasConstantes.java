package ve.edu.ucab.poo.modelo;

import java.util.regex.Pattern;

public class ReglasConstantes {
    public static final int TOTAL_RONDAS = 8;
    public static final int PUNTOS_ACCION_POR_TURNO = 3;
    public static final int CAPACIDAD_MAXIMA_INVENTARIO = 2;
    public static final int COSTO_MOVIMIENTO_NORMAL = 1;
    public static final int COSTO_MOVIMIENTO_PANTANO = 2;
    public static final int COSTO_EXPLORAR = 1;
    public static final int COSTO_ENTREGAR = 1;
    public static final int COSTO_ABRIR_COFRE = 1;

    public static final Pattern PATRON_CONTRASENA_COFRE = Pattern.compile("^[A-Z0-9]+(?:-[A-Z0-9]+)?$");
    public static final Pattern PATRON_NOMBRE_JUGADOR = Pattern.compile("^.{1,80}$");
    public static final Pattern PATRON_ALIAS_JUGADOR = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");
}