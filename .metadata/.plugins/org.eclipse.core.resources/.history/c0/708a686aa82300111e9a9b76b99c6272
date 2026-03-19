package com.emailAI.service;

import dev.langchain4j.model.openai.OpenAiChatModel;

public class IAAsistenteService {
    
    private final OpenAiChatModel model;

    public IAAsistenteService(String apiKey) {
        this.model = OpenAiChatModel.withApiKey(apiKey);
    }

    public String generarResumen(String cuerpoCorreo) {
        if (cuerpoCorreo == null || cuerpoCorreo.length() < 200) return cuerpoCorreo;
        
        String prompt = "Resume el siguiente correo electrónico en una sola frase clara y directa: " + cuerpoCorreo;
        return model.generate(prompt);
    }

    public String sugerirRespuesta(String cuerpoCorreo) {
        String prompt = "Basado en este correo, sugiere una respuesta corta y profesional: " + cuerpoCorreo;
        return model.generate(prompt);
    }
}