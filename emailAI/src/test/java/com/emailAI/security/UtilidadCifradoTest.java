package com.emailAI.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilidadCifradoTest {

    @Test
    void cifrarYDescifrar_debeRecuperarTextoOriginal() {
        String original = "correo+prueba@dominio.com";
        String cifrado = UtilidadCifrado.cifrar(original);
        String descifrado = UtilidadCifrado.descifrar(cifrado);

        assertNotEquals(original, cifrado);
        assertEquals(original, descifrado);
    }

    @Test
    void hash_debeSerDeterminista() {
        String texto = "mi-password-segura";
        String hash1 = UtilidadCifrado.hash(texto);
        String hash2 = UtilidadCifrado.hash(texto);

        assertEquals(hash1, hash2);
        assertNotNull(hash1);
    }
}
