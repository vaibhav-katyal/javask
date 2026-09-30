package io.github.vaibhavkatyal.javask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON support for the dependency-free client. */
final class Json {
    private Json() { }

    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        for (int n = hex.length(); n < 4; n++) out.append('0');
                        out.append(hex);
                    } else out.append(c);
            }
        }
        return out.append('"').toString();
    }

    static String answer(String body) {
        try {
            Map<?, ?> root = (Map<?, ?>) new Parser(body).parse();
            List<?> choices = (List<?>) root.get("choices");
            Map<?, ?> first = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) first.get("message");
            Object content = message.get("content");
            return content instanceof String ? (String) content : null;
        } catch (RuntimeException e) {
            throw new GroqException("Groq returned an invalid chat response.", e);
        }
    }

    static Object parse(String body) {
        return new Parser(body).parse();
    }

    static String errorMessage(String body) {
        try {
            Map<?, ?> root = (Map<?, ?>) new Parser(body).parse();
            Map<?, ?> error = (Map<?, ?>) root.get("error");
            Object message = error.get("message");
            return message instanceof String ? (String) message : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) { this.text = text; }

        Object parse() {
            Object value = value();
            space();
            if (pos != text.length()) throw bad();
            return value;
        }

        private Object value() {
            space();
            if (pos == text.length()) throw bad();
            char c = text.charAt(pos);
            if (c == '"') return string();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == 't') { literal("true"); return Boolean.TRUE; }
            if (c == 'f') { literal("false"); return Boolean.FALSE; }
            if (c == 'n') { literal("null"); return null; }
            if (c == '-' || Character.isDigit(c)) return number();
            throw bad();
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            pos++;
            space();
            if (take('}')) return result;
            do {
                space();
                if (pos >= text.length() || text.charAt(pos) != '"') throw bad();
                String key = string();
                space();
                expect(':');
                result.put(key, value());
                space();
                if (take('}')) return result;
                expect(',');
            } while (true);
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<Object>();
            pos++;
            space();
            if (take(']')) return result;
            do {
                result.add(value());
                space();
                if (take(']')) return result;
                expect(',');
            } while (true);
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (pos >= text.length()) throw bad();
                    char e = text.charAt(pos++);
                    switch (e) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case '/': out.append('/'); break;
                        case 'b': out.append('\b'); break;
                        case 'f': out.append('\f'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'u':
                            if (pos + 4 > text.length()) throw bad();
                            try { out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16)); }
                            catch (NumberFormatException ex) { throw bad(); }
                            pos += 4;
                            break;
                        default: throw bad();
                    }
                } else {
                    if (c < 0x20) throw bad();
                    out.append(c);
                }
            }
            throw bad();
        }

        private String number() {
            int start = pos;
            while (pos < text.length() && "-+0123456789.eE".indexOf(text.charAt(pos)) >= 0) pos++;
            String value = text.substring(start, pos);
            try { Double.parseDouble(value); }
            catch (NumberFormatException e) { throw bad(); }
            return value;
        }

        private void literal(String expected) {
            if (!text.startsWith(expected, pos)) throw bad();
            pos += expected.length();
        }

        private void space() {
            while (pos < text.length() && " \r\n\t".indexOf(text.charAt(pos)) >= 0) pos++;
        }

        private void expect(char c) { if (!take(c)) throw bad(); }
        private boolean take(char c) {
            if (pos < text.length() && text.charAt(pos) == c) { pos++; return true; }
            return false;
        }
        private IllegalArgumentException bad() { return new IllegalArgumentException("Invalid JSON at " + pos); }
    }
}
