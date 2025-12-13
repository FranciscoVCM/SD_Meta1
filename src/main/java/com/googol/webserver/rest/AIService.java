package com.googol.webserver.rest;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

@Service
public class AIService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma2:2b";

    public String analyzeSearch(String query, List<String> snippets) {

        if (query == null || query.isBlank()) {
            return "Nenhuma análise disponível.";
        }

        // Construir texto seguro
        StringBuilder sb = new StringBuilder();
        sb.append("Termo pesquisado: ").append(query).append("\n");
        sb.append("Trechos:\n");

        if (snippets != null) {
            for (String s : snippets) {
                if (s != null && !s.isBlank())
                    sb.append("- ").append(s.replace("\n", " ")).append("\n");
            }
        }

        String prompt = """
                Faça uma análise curta (máx. 80 palavras) com base no termo e nos trechos apresentados.

                %s
                """.formatted(sb.toString());

        try {
            URL url = URI.create(OLLAMA_URL).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(8000);
            con.setReadTimeout(20000);
            con.setRequestProperty("Content-Type", "application/json");

            // JSON ESCAPADO CORRETAMENTE
            String payload = """
            {
              "model": "%s",
              "prompt": "%s",
              "stream": false
            }
            """.formatted(MODEL, jsonEscape(prompt));

            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes());
            }

            int code = con.getResponseCode();
            if (code != 200) {
                return "⚠ Erro do modelo (HTTP " + code + ")";
            }

            // Ler JSON
            StringBuilder resp = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null)
                    resp.append(line);
            }

            return extractResponse(resp.toString());

        } catch (IOException e) {
            return """
            ⚠ IA indisponível.

            Verifica:
              → ollama serve está a correr
              → tens o modelo gemma2:2b instalado
            """;
        }
    }

    // ESCAPE JSON COMPLETO
    private String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String extractResponse(String json) {
        int i = json.indexOf("\"response\"");
        if (i < 0) return "Erro ao interpretar resposta da IA.";

        int start = json.indexOf("\"", i + 11) + 1;
        int end = json.indexOf("\"", start);
        if (start < 0 || end < 0) return "Erro ao ler resposta da IA.";

        return json.substring(start, end)
                .replace("\\n", "\n")
                .trim();
    }
}

