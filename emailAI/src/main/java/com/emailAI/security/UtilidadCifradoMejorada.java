package com.emailAI.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utilidad de cifrado AES-256 con PBKDF2 para derivación de clave y IV aleatorio.
 * Proporciona seguridad robusta para almacenamiento de credenciales y datos sensibles.
 */
public class UtilidadCifradoMejorada {

    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";
    private static final String ALGORITMO_CLAVE = "AES";
    private static final String ALGORITMO_DERIVACION = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 100000;
    private static final int LONGITUD_CLAVE = 256;
    private static final int LONGITUD_IV = 16;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Cifra un texto usando AES-256 con clave derivada de contraseña maestra.
     * Formato de salida: Base64(salt + iv + datos_cifrados)
     */
    public static String cifrar(String textoPlano, String contrasenaMaestra) {
        try {
            // Generar salt aleatorio
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Derivar clave usando PBKDF2
            SecretKey clave = derivarClave(contrasenaMaestra, salt);

            // Generar IV aleatorio
            byte[] iv = new byte[LONGITUD_IV];
            random.nextBytes(iv);

            // Cifrar datos
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, clave, ivSpec);
            byte[] datosCifrados = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            // Combinar salt + iv + datos cifrados
            byte[] resultado = new byte[salt.length + iv.length + datosCifrados.length];
            System.arraycopy(salt, 0, resultado, 0, salt.length);
            System.arraycopy(iv, 0, resultado, salt.length, iv.length);
            System.arraycopy(datosCifrados, 0, resultado, salt.length + iv.length, datosCifrados.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar datos", e);
        }
    }

    /**
     * Descifra un texto cifrado con AES-256.
     * Formato esperado: Base64(salt + iv + datos_cifrados)
     */
    public static String descifrar(String textoCifrado, String contrasenaMaestra) {
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifrado);

            // Extraer salt, iv y datos cifrados
            byte[] salt = new byte[16];
            byte[] iv = new byte[LONGITUD_IV];
            byte[] datosCifrados = new byte[datos.length - 32]; // 16 salt + 16 iv

            System.arraycopy(datos, 0, salt, 0, 16);
            System.arraycopy(datos, 16, iv, 0, LONGITUD_IV);
            System.arraycopy(datos, 32, datosCifrados, 0, datosCifrados.length);

            // Derivar clave usando el mismo salt
            SecretKey clave = derivarClave(contrasenaMaestra, salt);

            // Descifrar datos
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, clave, ivSpec);
            byte[] datosDescifrados = cipher.doFinal(datosCifrados);

            return new String(datosDescifrados, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar datos", e);
        }
    }

    /**
     * Genera un hash SHA-256 del texto para verificación de integridad.
     */
    public static String hash(String texto) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash", e);
        }
    }

    /**
     * Deriva una clave AES-256 a partir de contraseña y salt usando PBKDF2.
     */
    private static SecretKey derivarClave(String contrasena, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(contrasena.toCharArray(), salt, ITERACIONES, LONGITUD_CLAVE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO_DERIVACION);
            byte[] claveBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(claveBytes, ALGORITMO_CLAVE);
        } catch (Exception e) {
            throw new RuntimeException("Error al derivar clave", e);
        }
    }

    /**
     * Genera una clave AES aleatoria (para uso interno o testing).
     */
    public static String generarClaveAleatoria() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITMO_CLAVE);
            keyGen.init(LONGITUD_CLAVE);
            SecretKey clave = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(clave.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar clave aleatoria", e);
        }
    }

    /**
     * Verifica la integridad de un texto comparando su hash.
     */
    public static boolean verificarIntegridad(String texto, String hashEsperado) {
        String hashCalculado = hash(texto);
        return hashCalculado.equals(hashEsperado);
    }
}
