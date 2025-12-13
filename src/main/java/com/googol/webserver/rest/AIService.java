package com.googol.webserver.rest;

import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma2:2b";

    private final SnippetStore snippetStore;

    public AIService(SnippetStore snippetStore) {
        this.snippetStore = snippetStore;
    }

    private final Map<String, Cached> cache = new HashMap<>();

    private static class Cached {
        String analysis;
        int usedSnippets;
    }

    /** API PRINCIPAL — gera resumo incremental */
    public synchronized String analyze(String term, List<String> freshSnippets) {

        if (term == null || term.isBlank()) {
            return "Nenhuma análise disponível.";
        }

        // 1) adicionar snippets novos ao armazenamento global
        List<String> allSnippets = snippetStore.addSnippets(term, freshSnippets);

        // 2) consultar cache
        Cached previous = cache.get(term);
        if (previous != null && allSnippets.size() <= previous.usedSnippets) {
            return previous.analysis; // não há snippets novos, devolve o resumo antigo
        }

        // 3) criar resumo novo
        String prompt = buildPrompt(term, allSnippets);
        String summary = callModel(prompt);

        // 4) guardar na cache
        Cached c = new Cached();
        c.analysis = summary;
        c.usedSnippets = allSnippets.size();
        cache.put(term, c);

        return summary;
    }

    /** ---------------- PROMPT ---------------- */

    private String buildPrompt(String term, List<String> snippets) {

        StringBuilder sb = new StringBuilder();
        sb.append("Termo pesquisado: ").append(term).append("\n\n");

        if (snippets.isEmpty()) {
            sb.append("Não existe qualquer snippet disponível.\n");
        } else {
            sb.append("Snippets recolhidos:\n");
            for (String s : snippets) {
                sb.append("- ").append(s.replace("\n", " ")).append("\n");
            }
        }

        return """
                Produza um resumo COMPLETO e claro, em português europeu, com cerca de 120 palavras.
                • Explique o tema de forma coerente.
                • Se houver snippets, utilize-os para enriquecer o resumo.
                • O texto deve ser fluido, natural, sem listas marcadas.
                • O resumo deve ser totalmente completo — nunca o interrompa a meio.
                • Não inclua barras invertidas nem códigos estranhos.

                Conteúdo fornecido:
                %s

                Resumo final:
                """.formatted(sb);
    }

    /** ---------------- OLLAMA CALL ---------------- */

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
            return "IA indisponível no momento.";
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

        return txt;
    }
}
