package com.emailAI;

import java.io.Console;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Scanner;

import com.emailAI.dao.ConexionBD;
import com.emailAI.dao.DAOCuentas;
import com.emailAI.dao.DAOEntrenamiento;
import com.emailAI.ia.ExtractorAtributos;
import com.emailAI.ia.GestorModelos;
import com.emailAI.model.Mensaje;
import com.emailAI.security.UtilidadCifrado;
import com.emailAI.service.MailService;

public class AppConsole {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        MailService service = new MailService();

        boolean debeGuardarCuenta = false;
        String hostImapParaGuardar = null;
        String emailParaGuardar = null;

        try {
            DAOCuentas daoCuentas = new DAOCuentas();

            String hostImap = "";
            String email = "";
            String passwordCorreo = "";

            // 1. ¿Hay cuenta guardada?
            try (Connection conn = ConexionBD.getConnection()) {
                ResultSet rs = daoCuentas.obtenerPrimeraCuenta(conn);

                if (rs.next()) {
                    String servidorImapGuardado = rs.getString("servidor_imap");
                    String emailCifrado = rs.getString("email_cifrado");

                    String emailDescifrado = UtilidadCifrado.descifrar(emailCifrado);

                    System.out.println("Se ha encontrado una cuenta guardada:");
                    System.out.println("  Servidor IMAP: " + servidorImapGuardado);
                    System.out.println("  Email: " + emailDescifrado);
                    System.out.print("¿Iniciar sesión con esta cuenta? (s/n): ");
                    String usar = scanner.nextLine().trim().toLowerCase();

                    if (usar.equals("s")) {
                        hostImap = servidorImapGuardado;
                        email = emailDescifrado;

                        Console console = System.console();
                        if (console != null) {
                            char[] passChars = console.readPassword("Password del correo: ");
                            passwordCorreo = new String(passChars);
                        } else {
                            System.out.print("Password del correo: ");
                            passwordCorreo = scanner.nextLine();
                        }

                    } else {
                        System.out.print("Servidor IMAP (ej: imap.gmail.com): ");
                        hostImap = scanner.nextLine();
                        System.out.print("Usuario (email completo): ");
                        email = scanner.nextLine();
                        System.out.print("Password del correo: ");
                        passwordCorreo = scanner.nextLine();

                        debeGuardarCuenta = true;
                        hostImapParaGuardar = hostImap;
                        emailParaGuardar = email;
                    }
                } else {
                    System.out.print("Servidor IMAP: ");
                    hostImap = scanner.nextLine();
                    System.out.print("Usuario: ");
                    email = scanner.nextLine();
                    System.out.print("Password: ");
                    passwordCorreo = scanner.nextLine();

                    debeGuardarCuenta = true;
                    hostImapParaGuardar = hostImap;
                    emailParaGuardar = email;
                }
            }

            // 2. Conexión
            String imapHost = hostImapParaGuardar != null ? hostImapParaGuardar : hostImap;
            String smtpHost = imapHost.replace("imap.", "smtp.");
            String emailFinal = emailParaGuardar != null ? emailParaGuardar : email;

            service.connect(imapHost, smtpHost, emailFinal, passwordCorreo);
            System.out.println("Conectado correctamente.\n");

            // 3. Guardar cuenta
            if (debeGuardarCuenta) {
                System.out.print("¿Guardar esta cuenta? (s/n): ");
                if (scanner.nextLine().trim().toLowerCase().equals("s")) {
                    String emailCif = UtilidadCifrado.cifrar(emailParaGuardar);
                    String hashD = UtilidadCifrado.hash(emailParaGuardar + "@" + hostImapParaGuardar);
                    daoCuentas.guardarCuenta(hostImapParaGuardar, smtpHost, emailCif, hashD);
                    System.out.println("Cuenta guardada.");
                }
            }

            // 4. Listar y clasificar
            List<Mensaje> mensajes = service.listInbox();

            for (Mensaje m : mensajes) {
                // Aquí usamos los métodos actualizados del GestorModelos
                String cat = GestorModelos.clasificarSpam(m);
                String prio = "NORMAL";
                if (!cat.equals("SPAM")) {
                    prio = GestorModelos.clasificarPrioridad(m);
                }

                System.out.println("----------------------------------------");
                System.out.println("[" + cat + " | " + prio + "] De: " + m.getRemitente());
                System.out.println("Asunto: " + m.getAsunto());
                String cuerpo = m.getCuerpo() != null ? m.getCuerpo() : "";
                if (cuerpo.length() > 100) cuerpo = cuerpo.substring(0, 100) + "...";
                System.out.println(cuerpo);
            }

            service.close();
        } catch (Exception e) {
            System.err.println("Error en la aplicación: " + e.getMessage());
        } finally {
            System.out.print("\n¿Entrenar modelos IA ahora? (s/n): ");
            String entrenar = scanner.nextLine().trim().toLowerCase();
            if (entrenar.equals("s")) {
                entrenarModelosDesdeBD();
            }
            scanner.close();
        }
    }

    private static void entrenarModelosDesdeBD() {
        try {
            DAOEntrenamiento dao = new DAOEntrenamiento();
            
            // Entrenar SPAM
            List<DAOEntrenamiento.Ejemplo> ejemplosSpam = dao.listarEjemplosPorTipo("SPAM");
            if (!ejemplosSpam.isEmpty()) {
                var data = ExtractorAtributos.convertirAEstructura(ejemplosSpam);
                GestorModelos.entrenarYGuardar(data, "SPAM");
                System.out.println("Modelo de SPAM entrenado.");
            }

            // Entrenar PRIORIDAD
            List<DAOEntrenamiento.Ejemplo> ejemplosPrio = dao.listarEjemplosPorTipo("PRIORIDAD");
            if (!ejemplosPrio.isEmpty()) {
                var data = ExtractorAtributos.convertirAEstructuraPrioridad(ejemplosPrio);
                GestorModelos.entrenarYGuardar(data, "PRIORIDAD");
                System.out.println("Modelo de PRIORIDAD entrenado.");
            }

        } catch (Exception e) {
            System.err.println("Error en entrenamiento: " + e.getMessage());
        }
    }
}