package ve.edu.ucab.poo;

import ve.edu.ucab.poo.cliente.ClienteApi;
import ve.edu.ucab.poo.controlador.ControladorExpedicion;
import ve.edu.ucab.poo.vista.VistaConsola;

public class Main {
    public static void main(String[] args) {
        String url = "http://localhost:8000/api/v1";
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            url = args[0].trim();
        }

        ClienteApi api = new ClienteApi(url);
        ControladorExpedicion ctrl = new ControladorExpedicion(api);
        VistaConsola vista = new VistaConsola(ctrl);
        vista.iniciar();
    }
}