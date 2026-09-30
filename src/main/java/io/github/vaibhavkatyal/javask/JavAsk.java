package io.github.vaibhavkatyal.javask;

/** One-line terminal helper for Groq questions. */
public final class JavAsk {
    private JavAsk() { }

    /** Asks Groq, prints the answer to standard output, and returns it. */
    public static String ques(String apiKey, String question) {
        String answer = new GroqClient(apiKey).answer(question);
        System.out.println(answer);
        return answer;
    }
}
