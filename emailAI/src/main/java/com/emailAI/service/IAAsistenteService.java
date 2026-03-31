package com.emailAI.service;

import dev.langchain4j.model.openai.OpenAiChatModel;

// Servicio LLM para generar resúmenes y respuestas sugeridas de correos.
public class IAAsistenteService {

    private final OpenAiChatModel model; // Cliente de chat model de LangChain4j.

    // Construye el cliente del modelo remoto usando la API key proporcionada.
    public IAAsistenteService(String apiKey) {
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini") // cambia si quieres otro modelo [web:77][web:107]
                .build();
    }

    // Devuelve un resumen breve del correo o el texto original si ya es corto.
    public String generarResumen(String cuerpoCorreo) {
        if (cuerpoCorreo == null || cuerpoCorreo.length() < 200) {
            return cuerpoCorreo;
        }

        String prompt = "Resume el siguiente correo electrónico en una sola frase clara y directa:\n\n"
                + cuerpoCorreo;

        return model.chat(prompt);
    }

    // Genera una propuesta de respuesta profesional basada en el contenido del correo.
    public String sugerirRespuesta(String cuerpoCorreo) {
        String prompt = "Basado en este correo, sugiere una respuesta corta y profesional en español:\n\n"
                + cuerpoCorreo;

        return model.chat(prompt);
    }
}
