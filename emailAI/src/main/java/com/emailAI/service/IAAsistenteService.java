package com.emailAI.service;

import dev.langchain4j.model.openai.OpenAiChatModel;

public class IAAsistenteService {

    private final OpenAiChatModel model;

    public IAAsistenteService(String apiKey) {
        this.model = OpenAiChatModel.withApiKey(apiKey);
    }

    public String generarResumen(String cuerpoCorreo) {
        if (cuerpoCorreo == null || cuerpoCorreo.length() < 200) return cuerpoCorreo;

        String prompt = "Resume el siguiente correo electrónico en una sola frase clara y directa en español:\n\n"
                + cuerpoCorreo;
        return model.generate(prompt);
    }

    public String sugerirRespuesta(String cuerpoCorreo) {
        String prompt = "Basado en este correo, sugiere una respuesta corta y profesional en español:\n\n"
                + cuerpoCorreo;
        return model.generate(prompt);
    }

    // ===== Nuevo: soporte para chat IA general =====

    public String chatear(String mensajeUsuario) {
        if (mensajeUsuario == null || mensajeUsuario.isBlank()) {
            return "No he recibido ningún mensaje. Escríbeme algo.";
        }

        String prompt = """
                Eres un asistente integrado en un cliente de correo electrónico de escritorio.
                Ayudas con:
                - gestión de correos (spam, phishing, resúmenes, respuestas),
                - calendario,
                - tareas y contactos.

                Responde de forma breve, clara y en español.

                Mensaje del usuario:
                """ + mensajeUsuario;

        return model.generate(prompt);
    }
}
