package ve.edu.ucab.poo.modelo;

public enum TipoTerreno {
    CAMPAMENTO("C", "Campamento", ReglasConstantes.COSTO_MOVIMIENTO_NORMAL),
    SENDERO("S", "Sendero", ReglasConstantes.COSTO_MOVIMIENTO_NORMAL),
    PANTANO("P", "Pantano", ReglasConstantes.COSTO_MOVIMIENTO_PANTANO),
    TEMPLO_NORMAL("T", "Templo", ReglasConstantes.COSTO_MOVIMIENTO_NORMAL),
    TEMPLO_COFRE("X", "Cofre", ReglasConstantes.COSTO_MOVIMIENTO_NORMAL);

    private String simbolo;
    private String nombre;
    private int costoEntrada;

    TipoTerreno(String simbolo, String nombre, int costoEntrada) {
        this.simbolo = simbolo;
        this.nombre = nombre;
        this.costoEntrada = costoEntrada;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCostoEntrada() {
        return costoEntrada;
    }

    public static TipoTerreno desdeSimbolo(String s) {
        for (TipoTerreno t : values()) {
            if (t.simbolo.equalsIgnoreCase(s)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Terreno desconocido: " + s);
    }
}