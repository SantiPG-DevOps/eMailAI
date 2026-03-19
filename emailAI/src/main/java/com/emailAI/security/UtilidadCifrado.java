package com.emailAI.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class UtilidadCifrado {

    // Clave fija legacy (para lo que ya la use)
    private static final String CLAVE_FIJA = "emailAI-demo-clave";

    // ===== API NUEVA: cifrado con clave por cuenta =====

    public static String cifrar(String texto, String clave) {
        if (texto == null) return null;
        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        byte[] claveBytes = claveSegura(clave);
        byte[] resultado = new byte[datos.length];

        for (int i = 0; i < datos.length; i++) {
            resultado[i] = (byte) (datos[i] ^ claveBytes[i % claveBytes.length]);
        }

        return Base64.getEncoder().encodeToString(resultado);
    }

    public static String descifrar(String cifrado, String clave) {
        if (cifrado == null) return null;
        byte[] datos = Base64.getDecoder().decode(cifrado);
        byte[] claveBytes = claveSegura(clave);
        byte[] resultado = new byte[datos.length];

        for (int i = 0; i < datos.length; i++) {
            resultado[i] = (byte) (datos[i] ^ claveBytes[i % claveBytes.length]);
        }

        return new String(resultado, StandardCharsets.UTF_8);
    }

    // Derivar bytes de clave a partir de un String (hash SHA-256)
    private static byte[] claveSegura(String clave) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(clave.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // fallback: clave UTF-8 directa
            return clave.getBytes(StandardCharsets.UTF_8);
        }
    }

    // ===== API LEGACY: usa CLAVE_FIJA (para código antiguo) =====

    public static String cifrar(String texto) {
        return cifrar(texto, CLAVE_FIJA);
    }

    public static String descifrar(String cifrado) {
        return descifrar(cifrado, CLAVE_FIJA);
    }

    public static String hash(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
