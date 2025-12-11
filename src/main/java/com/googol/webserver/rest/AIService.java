package com.googol.webserver.rest;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Service
public class AIService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public String summarize(String text) {
        try {
            URL url = URI.create(OLLAMA_URL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Modelo leve que corre em qualquer máquina
            String model = "gemma2:2b";

            String payload = """
            {
                "model": "%s",
                "prompt": "Resume o seguinte texto de forma clara e curta:\\n%s",
                "stream": false
            }
            """.formatted(model, text.replace("\"", "\\\""));

            OutputStream os = connection.getOutputStream();
            os.write(payload.getBytes());
            os.flush();
            os.close();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String response = reader.readLine();
            response = response.replace("\\n", "\n");
            reader.close();

            // Extrair apenas o campo "response"
            return response.replaceAll(".*\"response\"\\s*:\\s*\"(.*?)\".*", "$1");

        } catch (Exception e) {
            return "Erro ao contactar IA: " + e.getMessage();
        }
    }
}
