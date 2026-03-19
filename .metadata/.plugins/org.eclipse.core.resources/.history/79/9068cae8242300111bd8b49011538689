package com.emailAI.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class UtilidadCifrado {

    // Clave fija de ejemplo: CAMBIAR en tu proyecto real
    private static final String CLAVE = "emailAI-demo-clave";

    public static String cifrar(String texto) {
        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        byte[] clave = CLAVE.getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[datos.length];

        for (int i = 0; i < datos.length; i++) {
            resultado[i] = (byte) (datos[i] ^ clave[i % clave.length]);
        }

        return Base64.getEncoder().encodeToString(resultado);
    }

    public static String descifrar(String cifrado) {
        byte[] datos = Base64.getDecoder().decode(cifrado);
        byte[] clave = CLAVE.getBytes(StandardCharsets.UTF_8);
        byte[] resultado = new byte[datos.length];

        for (int i = 0; i < datos.length; i++) {
            resultado[i] = (byte) (datos[i] ^ clave[i % clave.length]);
        }

        return new String(resultado, StandardCharsets.UTF_8);
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

