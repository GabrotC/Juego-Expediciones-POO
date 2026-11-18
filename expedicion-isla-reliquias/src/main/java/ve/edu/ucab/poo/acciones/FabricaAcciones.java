package ve.edu.ucab.poo.acciones;

import ve.edu.ucab.poo.criptografia.ServicioCriptografico;

public class FabricaAcciones {
    private ServicioCriptografico servicioCriptografico;

    public FabricaAcciones(ServicioCriptografico servicioCriptografico) {
        this.servicioCriptografico = servicioCriptografico;
    }

    public AccionJuego crearAccionMover(String destino) {
        return new MoverAccion(destino);
    }

    public AccionJuego crearAccionExplorar(ExplorarTemploAccion.ProveedorContenidoTemplo proveedor) {
        return new ExplorarTemploAccion(proveedor);
    }

    public AccionJuego crearAccionEntregar() {
        return new EntregarReliquiasAccion();
    }

    public AccionJuego crearAccionAbrirCofre(String contrasena,
                                             AbrirCofreAccion.ProveedorPaqueteCofre proveedorCofre) {
        return new AbrirCofreAccion(contrasena, proveedorCofre, servicioCriptografico);
    }
}