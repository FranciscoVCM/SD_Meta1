package com.googol.webserver.rest;

import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Service
public class AIService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma2:2b"; // podes trocar por llama3:8b etc

    public String summarize(String text) {

        // Caso texto seja vazio
        if (text == null || text.isBlank()) {
            return "Nenhum texto para resumir.";
        }

        try {
            URL url = URI.create(OLLAMA_URL).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setConnectTimeout(4000);
            con.setReadTimeout(15000);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            String payload = """
            {
                "model": "%s",
                "prompt": "Resuma o seguinte texto de forma clara e concisa:\\n%s",
                "stream": false
            }
            """.formatted(MODEL, escape(text));

            // enviar JSON
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes());
            }

            int code = con.getResponseCode();

            if (code != 200) {
                return "Erro: Ollama respondeu com código HTTP " + code;
            }

            // ler resposta inteira
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            // resposta vem como JSON → extrair campo "response"
            return extractResponse(sb.toString());

        } catch (IOException e) {
            return """
            ⚠ Não foi possível contactar o Ollama.

            Certifica-te que tens o Ollama a correr:
              →  abre terminal e executa:  ollama serve
            """;
        }
    }

    // --- Helpers -----------------------------------

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    /**
     * Extrai o campo "response" do JSON devolvido pelo Ollama.
     *
     * Formato típico:
     * {"model":"...","created_at":"...", "response":"TEXTO AQUI", ...}
     */
    private String extractResponse(String json) {
        int i = json.indexOf("\"response\"");
        if (i < 0) return "Erro: resposta inválida do modelo.";

        int start = json.indexOf("\"", i + 11) + 1;
        int end   = json.indexOf("\"", start);

        if (start < 0 || end < 0) return "Erro ao interpretar resposta da IA.";

        return json.substring(start, end)
                .replace("\\n", "\n")
                .trim();
    }
}
