package io.github.vaibhavkatyal.javask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Reusable Groq chat client. Calls the API synchronously. */
public final class GroqClient {
    public static final String DEFAULT_MODEL = "openai/gpt-oss-20b";
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private final String apiKey;
    private final String model;
    private final URL endpoint;

    /** Uses a supplied API key and the default model. */
    public GroqClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    /** Uses a supplied API key and Groq model ID. */
    public GroqClient(String apiKey, String model) {
        this(apiKey, model, url(ENDPOINT));
    }

    GroqClient(String apiKey, String model, URL endpoint) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Pass a non-empty Groq API key.");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model must not be empty.");
        }
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
    }

    /** Returns the assistant's text answer without printing it. */
    public String answer(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question must not be empty.");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            byte[] request = ("{\"model\":" + Json.quote(model)
                    + ",\"messages\":[{\"role\":\"user\",\"content\":"
                    + Json.quote(question) + "}],\"stream\":false}").getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(request);
            }
            int status = connection.getResponseCode();
            String body;
            try (InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                body = input == null ? "" : readLimited(input);
            }
            if (status < 200 || status >= 300) {
                String detail = Json.errorMessage(body);
                throw new GroqException("Groq HTTP " + status + (detail.isEmpty() ? "" : ": " + detail), status);
            }
            String answer = Json.answer(body);
            if (answer == null || answer.trim().isEmpty()) {
                throw new GroqException("Groq returned no text answer.", -1);
            }
            return answer;
        } catch (IOException e) {
            throw new GroqException("Could not contact Groq: " + e.getClass().getSimpleName(), e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > MAX_RESPONSE_BYTES) {
                throw new IOException("Response too large");
            }
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static URL url(String value) {
        try { return new URL(value); }
        catch (IOException e) { throw new IllegalStateException(e); }
    }
}
