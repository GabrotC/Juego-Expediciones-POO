package ve.edu.ucab.poo.criptografia;

public class PaqueteCifradoCofre {
    private String partidaId;
    private String cofreId;
    private String algoritmo;
    private String kdf;
    private int iteraciones;
    private int longitudClaveBits;
    private String saltBase64;
    private String nonceBase64;
    private int tagBits;
    private String aadBase64;
    private String cifradoConTagBase64;

    public PaqueteCifradoCofre() {
    }

    public PaqueteCifradoCofre(String partidaId, String cofreId, String algoritmo, String kdf,
                               int iteraciones, int longitudClaveBits, String saltBase64,
                               String nonceBase64, int tagBits, String aadBase64, String cifradoConTagBase64) {
        this.partidaId = partidaId;
        this.cofreId = cofreId;
        this.algoritmo = algoritmo;
        this.kdf = kdf;
        this.iteraciones = iteraciones;
        this.longitudClaveBits = longitudClaveBits;
        this.saltBase64 = saltBase64;
        this.nonceBase64 = nonceBase64;
        this.tagBits = tagBits;
        this.aadBase64 = aadBase64;
        this.cifradoConTagBase64 = cifradoConTagBase64;
    }

    public String getPartidaId() { return partidaId; }
    public String getCofreId() { return cofreId; }
    public String getAlgoritmo() { return algoritmo; }
    public String getKdf() { return kdf; }
    public int getIteraciones() { return iteraciones; }
    public int getLongitudClaveBits() { return longitudClaveBits; }
    public String getSaltBase64() { return saltBase64; }
    public String getNonceBase64() { return nonceBase64; }
    public int getTagBits() { return tagBits; }
    public String getAadBase64() { return aadBase64; }
    public String getCifradoConTagBase64() { return cifradoConTagBase64; }
}