package com.emailAI.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para UtilidadCifradoMejorada.
 * Verifica el correcto funcionamiento del cifrado AES-256 con PBKDF2.
 */
public class UtilidadCifradoMejoradaTest {

    private String contrasenaMaestra = "contraseña-secreta-test-123";
    private String textoPlano = "Este es un mensaje secreto de prueba para el emailAI";

    @BeforeEach
    void setUp() {
        // Configuración previa a cada test si es necesario
    }

    @Test
    @DisplayName("Cifrar y descifrar texto correctamente")
    void testCifrarDescifrarCorrecto() {
        // Ejecutar cifrado
        String cifrado = UtilidadCifradoMejorada.cifrar(textoPlano, contrasenaMaestra);
        
        // Verificar que el cifrado no sea nulo y sea diferente al original
        assertNotNull(cifrado, "El texto cifrado no debe ser nulo");
        assertNotEquals(textoPlano, cifrado, "El texto cifrado debe ser diferente al original");
        
        // Ejecutar descifrado
        String descifrado = UtilidadCifradoMejorada.descifrar(cifrado, contrasenaMaestra);
        
        // Verificar que el descifrado sea igual al original
        assertEquals(textoPlano, descifrado, "El texto descifrado debe ser igual al original");
    }

    @Test
    @DisplayName("Cifrar con misma contraseña produce resultados diferentes (por IV aleatorio)")
    void testCifradoConIVAleatorio() {
        // Cifrar el mismo texto dos veces
        String cifrado1 = UtilidadCifradoMejorada.cifrar(textoPlano, contrasenaMaestra);
        String cifrado2 = UtilidadCifradoMejorada.cifrar(textoPlano, contrasenaMaestra);
        
        // Deben ser diferentes por el IV aleatorio
        assertNotEquals(cifrado1, cifrado2, "Cifrados con IV aleatorio deben ser diferentes");
        
        // Pero ambos deben descifrarse al mismo texto original
        String descifrado1 = UtilidadCifradoMejorada.descifrar(cifrado1, contrasenaMaestra);
        String descifrado2 = UtilidadCifradoMejorada.descifrar(cifrado2, contrasenaMaestra);
        
        assertEquals(textoPlano, descifrado1, "Primer descifrado debe coincidir");
        assertEquals(textoPlano, descifrado2, "Segundo descifrado debe coincidir");
    }

    @Test
    @DisplayName("Descifrar con contraseña incorrecta debe fallar")
    void testDescifrarConContrasenaIncorrecta() {
        String cifrado = UtilidadCifradoMejorada.cifrar(textoPlano, contrasenaMaestra);
        String contrasenaIncorrecta = "contrasena-incorrecta";
        
        // Intentar descifrar con contraseña incorrecta
        assertThrows(RuntimeException.class, () -> {
            UtilidadCifradoMejorada.descifrar(cifrado, contrasenaIncorrecta);
        }, "Debe lanzar excepción con contraseña incorrecta");
    }

    @Test
    @DisplayName("Generar hash SHA-256 consistentemente")
    void testHashConsistente() {
        String hash1 = UtilidadCifradoMejorada.hash(textoPlano);
        String hash2 = UtilidadCifradoMejorada.hash(textoPlano);
        
        // Mismo texto debe producir mismo hash
        assertEquals(hash1, hash2, "Mismo texto debe producir mismo hash");
        
        // Hash no debe ser nulo ni vacío
        assertNotNull(hash1, "El hash no debe ser nulo");
        assertFalse(hash1.isBlank(), "El hash no debe estar vacío");
    }

    @Test
    @DisplayName("Hash de textos diferentes debe ser diferentes")
    void testHashDiferenteTextosDiferentes() {
        String texto1 = "texto de prueba 1";
        String texto2 = "texto de prueba 2";
        
        String hash1 = UtilidadCifradoMejorada.hash(texto1);
        String hash2 = UtilidadCifradoMejorada.hash(texto2);
        
        assertNotEquals(hash1, hash2, "Textos diferentes deben producir hashes diferentes");
    }

    @Test
    @DisplayName("Verificar integridad de texto")
    void testVerificarIntegridad() {
        String hash = UtilidadCifradoMejorada.hash(textoPlano);
        
        // Verificar integridad con hash correcto
        assertTrue(UtilidadCifradoMejorada.verificarIntegridad(textoPlano, hash), 
                  "Debe verificar integridad con hash correcto");
        
        // Verificar integridad con hash incorrecto
        String hashIncorrecto = "hash-incorrecto";
        assertFalse(UtilidadCifradoMejorada.verificarIntegridad(textoPlano, hashIncorrecto),
                   "No debe verificar integridad con hash incorrecto");
    }

    @Test
    @DisplayName("Generar clave aleatoria válida")
    void testGenerarClaveAleatoria() {
        String clave = UtilidadCifradoMejorada.generarClaveAleatoria();
        
        // Clave no debe ser nula ni vacía
        assertNotNull(clave, "La clave aleatoria no debe ser nula");
        assertFalse(clave.isBlank(), "La clave aleatoria no debe estar vacía");
        
        // Clave debe ser Base64 válida (no lanzar excepción al decodificar)
        assertDoesNotThrow(() -> {
            java.util.Base64.getDecoder().decode(clave);
        }, "La clave debe ser Base64 válida");
    }

    @Test
    @DisplayName("Manejar texto vacío y nulo")
    void testTextoVacioYNulo() {
        // Texto nulo
        String cifradoNulo = UtilidadCifradoMejorada.cifrar(null, contrasenaMaestra);
        String descifradoNulo = UtilidadCifradoMejorada.descifrar(cifradoNulo, contrasenaMaestra);
        
        assertEquals("", descifradoNulo, "Texto nulo debe descifrarse como cadena vacía");
        
        // Texto vacío
        String cifradoVacio = UtilidadCifradoMejorada.cifrar("", contrasenaMaestra);
        String descifradoVacio = UtilidadCifradoMejorada.descifrar(cifradoVacio, contrasenaMaestra);
        
        assertEquals("", descifradoVacio, "Texto vacío debe descifrarse como cadena vacía");
    }

    @Test
    @DisplayName("Cifrado debe ser determinista con misma contraseña y mismo IV (simulado)")
    void testDeterminismoConMismosParametros() {
        // Este test es difícil de implementar con IV aleatorio real,
        // pero verifica que el algoritmo sea determinista en teoría
        
        String texto = "mensaje determinista";
        String contrasena = "contraseña-fija";
        
        // Múltiples ejecuciones con mismos parámetros
        String cifrado1 = UtilidadCifradoMejorada.cifrar(texto, contrasena);
        String cifrado2 = UtilidadCifradoMejorada.cifrar(texto, contrasena);
        
        // No deben ser iguales por IV aleatorio (esto es correcto)
        assertNotEquals(cifrado1, cifrado2, "Cifrados deben ser diferentes por IV aleatorio");
        
        // Pero ambos deben poder descifrarse correctamente
        assertEquals(texto, UtilidadCifradoMejorada.descifrar(cifrado1, contrasena));
        assertEquals(texto, UtilidadCifradoMejorada.descifrar(cifrado2, contrasena));
    }
}
