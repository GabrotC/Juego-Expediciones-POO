package ve.edu.ucab.poo;

import org.junit.jupiter.api.Test;
import ve.edu.ucab.poo.acciones.*;
import ve.edu.ucab.poo.criptografia.PaqueteCifradoCofre;
import ve.edu.ucab.poo.criptografia.ServicioCriptografico;
import ve.edu.ucab.poo.acciones.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de los Casos de Aceptación (CA02..CA07) y del Vector de Prueba del Anexo B.
 */
public class AceptacionExpedicionTest {

    private PaqueteCifradoCofre crearVectorAnexoB() {
        return new PaqueteCifradoCofre(
                "EX-RED-01",
                "COFRE-01",
                "AES/GCM/NoPadding",
                "PBKDF2WithHmacSHA256",
                100000,
                256,
                "xBxRniX+mjbP3g16qy93zA==",
                "BVmBDNDCL0Gw5CM6",
                128,
                "RVgtUkVELTAxfENPRlJFLTAx",
                "u/hosFuUuUt+RKGBQV9S/amTmkZd5SwCLtnzyn6/EgT9Y3Nmhs7G9JfRfXMZPce426aLkJRpuGdof1jG0pikC+bkl1DmSNPrLIES17AuJ9UZaWvXl2pgbL84kWXKITU5noqdPQ=="
        );
    }

    private Partida crearPartidaAnexoB() {
        String[][] tipos = {
                {"S", "T", "S", "P", "S"},
                {"P", "S", "S", "X", "S"},
                {"S", "T", "C", "S", "P"},
                {"S", "S", "T", "S", "S"},
                {"T", "P", "S", "S", "S"}
        };
        List<Casilla> casillas = new ArrayList<>();
        for (int f = 0; f < 5; f++) {
            for (int c = 0; c < 5; c++) {
                String coord = "" + (char) ('A' + c) + (f + 1);
                casillas.add(new Casilla(f, c, coord, TipoTerreno.desdeSimbolo(tipos[f][c])));
            }
        }
        Tablero tablero = new Tablero(5, 5, casillas);
        Participante p1 = new Participante(new Jugador("J1", "Ana Pérez", "ana_ucab"), "C3");
        Participante p2 = new Participante(new Jugador("J2", "Luis Gómez", "luis_ucab"), "C3");
        return new Partida("EX-RED-01", "COFRE-01", "C3", "D2", tablero, List.of(p1, p2), 1);
    }

    @Test
    void testCA05_CA06_CA07_VectorAnexoBCofre() throws Exception {
        Partida partida = crearPartidaAnexoB();
        ServicioCriptografico crypto = new ServicioCriptografico();
        FabricaAcciones fabrica = new FabricaAcciones(crypto);

        // Ubicar a J1 en D2 (Cofre) con ambas pistas descubiertas
        partida.getParticipanteActual().moverA("D2");
        partida.registrarTemploExplorado("B3", new Pista(1, "ORBITA"));
        partida.registrarTemploExplorado("C4", new Pista(2, "7392"));

        // CA06: Formato inválido "orbita 7392" NO consume acción
        AccionJuego intentoFormatoMalo = fabrica.crearAccionAbrirCofre("orbita 7392", pId -> crearVectorAnexoB());
        assertThrows(ReglaJuegoException.class, () -> intentoFormatoMalo.ejecutar(partida));
        assertEquals(3, partida.getAccionesRestantes());

        // CA06: Contraseña con formato válido pero incorrecta "ORBITA-0000" SÍ consume 1 acción
        AccionJuego intentoClaveIncorrecta = fabrica.crearAccionAbrirCofre("ORBITA-0000", pId -> crearVectorAnexoB());
        ResultadoAccion resFallo = intentoClaveIncorrecta.ejecutar(partida);
        assertFalse(resFallo.isExito());
        assertEquals(2, partida.getAccionesRestantes());
        assertFalse(partida.isCofreAbierto());

        // CA05: Contraseña correcta en minúsculas "orbita-7392" se normaliza, abre el cofre y suma REL-ESPECIAL (valor 4)
        AccionJuego intentoCorrecto = fabrica.crearAccionAbrirCofre("orbita-7392", pId -> crearVectorAnexoB());
        ResultadoAccion resExito = intentoCorrecto.ejecutar(partida);
        assertTrue(resExito.isExito());
        assertEquals(1, partida.getAccionesRestantes());
        assertTrue(partida.isCofreAbierto());
        assertEquals(1, partida.getParticipanteActual().getReliquiasInventario().size());
        assertEquals(4, partida.getParticipanteActual().getReliquiasInventario().get(0).getValor());
    }
}