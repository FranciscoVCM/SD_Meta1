package com.googol.webserver.rest;

import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;

@Service
public class AIService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma2:2b";

    /** Cache: termo → análise gerada */
    private final Map<String, CachedAnalysis> cache = new HashMap<>();

    private static class CachedAnalysis {
        String analysis;
        int usedSnippets;   // quantos snippets foram usados na última geração
    }

    /**
     * Processa resumos incrementais:
     * - Gera com 0 snippets (fallback)
     * - Atualiza quando chegam novos snippets, até 50
     */
    public synchronized String analyze(String term, List<String> newSnippets) {

        if (term == null || term.isBlank())
            return "Nenhuma análise disponível.";

        newSnippets = newSnippets == null ? List.of() : newSnippets.stream()
                .filter(s -> s != null && !s.isBlank())
                .limit(50)
                .toList();

        CachedAnalysis cached = cache.get(term);

        // Se já tínhamos e não há snippets novos → devolve a mesma análise
        if (cached != null && (newSnippets.size() <= cached.usedSnippets)) {
            return cached.analysis;
        }

        // Construir novo prompt
        StringBuilder sb = new StringBuilder();
        sb.append("Termo pesquisado: ").append(term).append("\n");

        if (!newSnippets.isEmpty()) {
            sb.append("Trechos relevantes:\n");
            for (String s : newSnippets) {
                sb.append("- ").append(s.replace("\n", " ")).append("\n");
            }
        } else {
            sb.append("Não há trechos disponíveis.\n");
        }

        String prompt = """
                Gere um resumo curto (máximo 500 caracteres) em português.
                O resumo deve explicar o tema pesquisado, usando snippets se existirem.

                Conteúdo:
                %s
                """.formatted(sb);

        String answer = callModel(prompt);

        // Guardar na cache
        CachedAnalysis c = new CachedAnalysis();
        c.analysis = answer;
        c.usedSnippets = newSnippets.size();
        cache.put(term, c);

        return answer;
    }

    /** ------------------------ CHAMADA AO OLLAMA ------------------------- */
    private String callModel(String prompt) {

        try {
            URL url = URI.create(OLLAMA_URL).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

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

            if (con.getResponseCode() != 200) {
                return "⚠ Erro ao gerar resumo.";
            }

            StringBuilder json = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                while ((line = br.readLine()) != null)
                    json.append(line);
            }

            return extract(json.toString());

        } catch (Exception e) {
            return "⚠ IA indisponível no momento.";
        }
    }

    private String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String extract(String json) {
        int i = json.indexOf("\"response\"");
        if (i < 0) return "Erro ao interpretar resposta da IA.";

        int start = json.indexOf("\"", i + 11) + 1;
        int end = json.indexOf("\"", start);
        if (start < 0 || end < 0) return "Erro ao ler resposta da IA.";

        String txt = json.substring(start, end)
                .replace("\\n", "\n")
                .trim();

        if (txt.length() > 500)
            txt = txt.substring(0, 500) + "...";

        return txt;
    }
}
