package ve.edu.ucab.poo.criptografia;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ve.edu.ucab.poo.excepciones.AutenticacionCofreException;
import ve.edu.ucab.poo.excepciones.DatosCorruptosCofreException;
import ve.edu.ucab.poo.excepciones.ReglaJuegoException;
import ve.edu.ucab.poo.modelo.ReglasConstantes;
import ve.edu.ucab.poo.modelo.Reliquia;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ServicioCriptografico {

    public String normalizarYValidarFormatoContrasena(String entrada) throws ReglaJuegoException {
        if (entrada == null) {
            throw new ReglaJuegoException("Formato de contrasena invalido (no consume accion).");
        }
        String mayus = entrada.toUpperCase();
        if (mayus.length() < 4 || mayus.length() > 20
                || !ReglasConstantes.PATRON_CONTRASENA_COFRE.matcher(mayus).matches()) {
            throw new ReglaJuegoException("Formato de contrasena invalido (no consume accion).");
        }
        return mayus;
    }

    public Reliquia descifrarCofre(PaqueteCifradoCofre paquete, String partidaIdEsperada,
                                   String cofreIdEsperado, String contrasena)
            throws DatosCorruptosCofreException, AutenticacionCofreException {

        if (paquete == null || paquete.getPartidaId() == null || paquete.getCofreId() == null
                || paquete.getSaltBase64() == null || paquete.getNonceBase64() == null
                || paquete.getAadBase64() == null || paquete.getCifradoConTagBase64() == null) {
            throw new DatosCorruptosCofreException("Paquete del cofre incompleto.");
        }

        if (!partidaIdEsperada.equals(paquete.getPartidaId()) || !cofreIdEsperado.equals(paquete.getCofreId())) {
            throw new DatosCorruptosCofreException("Identificadores de partida o cofre no coinciden.");
        }

        if (paquete.getIteraciones() != 100000 || paquete.getLongitudClaveBits() != 256 || paquete.getTagBits() != 128) {
            throw new DatosCorruptosCofreException("Parametros criptograficos invalidos.");
        }

        byte[] salt;
        byte[] nonce;
        byte[] aad;
        byte[] cifrado;

        try {
            salt = Base64.getDecoder().decode(paquete.getSaltBase64().trim());
            nonce = Base64.getDecoder().decode(paquete.getNonceBase64().trim());
            aad = Base64.getDecoder().decode(paquete.getAadBase64().trim());
            cifrado = Base64.getDecoder().decode(paquete.getCifradoConTagBase64().trim());
        } catch (IllegalArgumentException e) {
            throw new DatosCorruptosCofreException("Base64 invalido en el paquete del cofre.", e);
        }

        if (salt.length != 16 || nonce.length != 12 || cifrado.length <= 16) {
            throw new DatosCorruptosCofreException("Tamanos de salt, nonce o cifrado incorrectos.");
        }

        String aadEsperado = partidaIdEsperada + "|" + cofreIdEsperado;
        String aadRecibido = new String(aad, StandardCharsets.UTF_8);
        if (!aadEsperado.equals(aadRecibido)) {
            throw new DatosCorruptosCofreException("El AAD no coincide.");
        }

        byte[] bytesDescifrados;
        try {
            PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), salt, paquete.getIteraciones(), paquete.getLongitudClaveBits());
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(paquete.getTagBits(), nonce);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
            cipher.updateAAD(aad);

            bytesDescifrados = cipher.doFinal(cifrado);
        } catch (Exception e) {
            throw new AutenticacionCofreException("No se pudo autenticar el archivo.", e);
        }

        try {
            String texto = new String(bytesDescifrados, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(texto).getAsJsonObject();

            String pId = json.get("partidaId").getAsString();
            String cId = json.get("cofreId").getAsString();
            String relId = json.get("reliquiaId").getAsString();
            int valor = json.get("valor").getAsInt();

            if (!partidaIdEsperada.equals(pId) || !cofreIdEsperado.equals(cId)) {
                throw new DatosCorruptosCofreException("IDs internos del JSON no coinciden.");
            }
            return new Reliquia(relId, valor);
        } catch (DatosCorruptosCofreException e) {
            throw e;
        } catch (Exception e) {
            throw new DatosCorruptosCofreException("El contenido descifrado no es un JSON valido.", e);
        }
    }
}