package io.github.vaibhavkatyal.javask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Dependency-free integration checks with an in-memory HTTP connection. */
public final class GroqClientTest {
    public static void main(String[] args) throws Exception {
        MockConnection ok = connection(200, "{\"choices\":[{\"message\":{\"content\":"
                + Json.quote("Hello \u263a\nworld") + "}}]}");
        equal("Hello \u263a\nworld", client(ok).answer("Say \"hi\" \\ \u263a\n"));
        equal("POST", ok.getRequestMethod());
        equal("Bearer test-only", ok.authorization);
        Map<?, ?> body = (Map<?, ?>) Json.parse(ok.request.toString("UTF-8"));
        equal("test-model", body.get("model"));
        equal("Say \"hi\" \\ \u263a\n", ((Map<?, ?>) ((List<?>) body.get("messages")).get(0)).get("content"));

        try {
            client(connection(429, "{\"error\":{\"message\":\"rate limit\"}}")).answer("error");
            throw new AssertionError("Expected HTTP error");
        } catch (GroqException e) {
            equal(429, e.getStatusCode());
            if (!e.getMessage().contains("rate limit")) throw new AssertionError(e);
        }
        try {
            client(connection(200, "{broken")).answer("malformed");
            throw new AssertionError("Expected response error");
        } catch (GroqException e) { equal(-1, e.getStatusCode()); }
        try {
            client(ok).answer(" ");
            throw new AssertionError("Expected question validation");
        } catch (IllegalArgumentException expected) { }
        try {
            JavAsk.ques("", "hello");
            throw new AssertionError("Expected key validation");
        } catch (IllegalArgumentException expected) { }
        System.out.println("All JavAsk checks passed.");
    }

    private static GroqClient client(final MockConnection mock) throws Exception {
        URL url = new URL(null, "mock://groq/chat", new URLStreamHandler() {
            protected URLConnection openConnection(URL ignored) { return mock; }
        });
        return new GroqClient("test-only", "test-model", url);
    }

    private static MockConnection connection(int status, String body) throws Exception {
        return new MockConnection(status, body);
    }

    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static final class MockConnection extends HttpURLConnection {
        final ByteArrayOutputStream request = new ByteArrayOutputStream();
        final byte[] body;
        final int status;
        String authorization;

        MockConnection(int status, String body) throws Exception {
            super(new URL("http://localhost/"));
            this.status = status;
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }
        public void connect() { }
        public void disconnect() { }
        public boolean usingProxy() { return false; }
        public void setRequestProperty(String key, String value) {
            if (key.equals("Authorization")) authorization = value;
        }
        public OutputStream getOutputStream() { return request; }
        public int getResponseCode() { return status; }
        public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(body); }
        public InputStream getErrorStream() { return new ByteArrayInputStream(body); }
    }
}
