from cryptography.fernet import Fernet
import base64
import hashlib

class CryptoService:
    """
    Servicio de criptografía para manejar el cifrado de payloads y
    la seguridad en la comunicación con el cliente Java.
    """
    def __init__(self, secret_key: str = None):
        if secret_key:
            # Asegura que la llave tenga el formato de 32 bytes requerido por Fernet
            digest = hashlib.sha256(secret_key.encode()).digest()
            self.key = base64.urlsafe_b64encode(digest)
        else:
            self.key = Fernet.generate_key()

        self.cipher = Fernet(self.key)

    def encrypt_data(self, plaintext: str) -> str:
        """Cifra un texto plano y devuelve un string seguro."""
        encrypted_bytes = self.cipher.encrypt(plaintext.encode('utf-8'))
        return encrypted_bytes.decode('utf-8')

    def decrypt_data(self, ciphertext: str) -> str:
        """Descifra un texto cifrado de vuelta a texto plano."""
        decrypted_bytes = self.cipher.decrypt(ciphertext.encode('utf-8'))
        return decrypted_bytes.decode('utf-8')

# Instancia global por defecto para los servicios del backend
crypto_manager = CryptoService("isla-de-las-reliquias-ucab-poo-2026")