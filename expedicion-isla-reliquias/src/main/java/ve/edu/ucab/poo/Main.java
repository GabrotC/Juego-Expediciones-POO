package ve.edu.ucab.poo;

import ve.edu.ucab.poo.cliente.ClienteApi;
import ve.edu.ucab.poo.controlador.ControladorExpedicion;
import ve.edu.ucab.poo.vista.VistaConsola;

public class Main {
    public static void main(String[] args) {
        String urlBase = "http://localhost:8000/api/v1";
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            urlBase = args[0].trim();
        }

        ClienteApi cliente = new ClienteApi(urlBase);
        ControladorExpedicion controlador = new ControladorExpedicion(cliente);
        VistaConsola vista = new VistaConsola(controlador);
        vista.iniciar();
    }
}