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

    /**
     * Gera análise AI baseada nos snippets devolvidos pela pesquisa.
     */
    public String analyzeSearch(String query, List<String> snippets) {

        if (query == null || query.isBlank()) {
            return "";
        }

        // Construção do prompt
        StringBuilder sb = new StringBuilder();
        sb.append("Termo pesquisado: ").append(query).append("\n\n");
        sb.append("Trechos relevantes das páginas encontradas:\n");

        for (int i = 0; i < Math.min(snippets.size(), 5); i++) {
            sb.append("- ").append(snippets.get(i)).append("\n");
        }

        String prompt = """
                Produza uma análise curta (máximo 80 palavras)
                sobre o termo pesquisado, usando os trechos fornecidos.
                Foque-se numa explicação geral do tema, não num resumo das páginas.

                Conteúdo:
                %s
                """.formatted(sb);

        try {
            // Configurar ligação ao Ollama
            URL url = URI.create(OLLAMA_URL).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(15000);
            con.setRequestProperty("Content-Type", "application/json");

            // JSON moderno aceito pelo Ollama
            String payload = """
            {
                "model": "%s",
                "prompt": "%s",
                "stream": false
            }
            """.formatted(MODEL, escape(prompt));

            // Enviar prompt
            try (OutputStream os = con.getOutputStream()) {
                os.write(payload.getBytes());
            }

            // Validar resposta HTTP
            int status = con.getResponseCode();
            if (status != 200) {
                return "⚠ Erro do modelo (HTTP %d)".formatted(status);
            }

            // Ler conteúdo completo da resposta
            StringBuilder json = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null)
                    json.append(line);
            }

            return extractResponse(json.toString());

        } catch (IOException e) {
            return """
            ⚠ IA indisponível.

            Certifica-te que tens o Ollama a correr:

              → abre terminal e executa:  ollama serve

            Detalhes financeiros:
            """ + e.getMessage();
        }
    }

    /**
     * Escapa aspas dentro do prompt.
     */
    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    /**
     * Extrai apenas o campo "response" do JSON devolvido pelo Ollama.
     */
    private String extractResponse(String json) {

        if (json == null) return "(Resposta vazia)";

        int idx = json.indexOf("\"response\"");
        if (idx < 0) return "(Sem campo 'response' na resposta da IA)";

        int start = json.indexOf("\"", idx + 11) + 1;
        int end   = json.indexOf("\"", start);

        if (start <= 0 || end <= 0) return "(Erro ao interpretar JSON do Ollama)";

        return json.substring(start, end)
                .replace("\\n", "\n")
                .trim();
    }
}

