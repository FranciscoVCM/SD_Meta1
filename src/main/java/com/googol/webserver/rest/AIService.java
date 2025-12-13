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

        // Criar texto base para análise
        StringBuilder sb = new StringBuilder();
        sb.append("Termo de pesquisa: ").append(query).append("\n\n");
        sb.append("Trechos das páginas encontradas:\n");

        for (int i = 0; i < Math.min(5, snippets.size()); i++) {
            sb.append("- ").append(snippets.get(i)).append("\n");
        }

        String prompt = """
                Faça uma análise curta (máx. 80 palavras) sobre o tema pesquisado,
                baseada no termo e nos trechos das páginas apresentados abaixo.
                Não resuma cada página; produza uma visão geral do tópico.

                Conteúdo:
                %s
                """.formatted(sb);

        try {
            URL url = URI.create(OLLAMA_URL).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(15000);
            con.setRequestProperty("Content-Type", "application/json");

            String payload = """
            {
                "model": "%s",
                "prompt": "%s",
                "stream": false
            }
            """.formatted(MODEL, escape(prompt));

            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes());
            }

            int code = con.getResponseCode();
            if (code != 200) {
                return "⚠ Erro do modelo (HTTP " + code + ")";
            }

            // ler JSON inteiro
            StringBuilder resp = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null)
                    resp.append(line);
            }

            return extractResponse(resp.toString());

        } catch (IOException e) {
            return """
            ⚠ IA indisponível no momento.

            Certifica-te que tens o Ollama a correr:
              → abre terminal e executa:  ollama serve
            """;
        }
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
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
