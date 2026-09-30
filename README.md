# JavAsk

A small, dependency-free Java library for asking a question through Groq. `JavAsk.ques(apiKey, question)` prints the answer in the terminal and returns it as a `String`.

## Requirements

- Java 8 or newer **to run**; a JDK is needed only to compile your own program. The release JAR uses Java 8 class files.
- Internet access to `api.groq.com`, working HTTPS/TLS, and a Groq account/API key.
- The host application must allow outbound HTTP requests and third-party JARs. Some college coding platforms disallow these; this library cannot override those restrictions.

No Java library can run on literally every Java version. This project targets Java 8+ and does not require Maven in the consuming application.

## Quick start with a JAR

1. Build with a JDK and PowerShell: `./build.ps1`. This creates `out/javask-0.1.0.jar` and runs local tests. Alternatively, with Maven installed: `mvn package -Dgpg.skip=true`.
2. Open `examples/HelloJavAsk.java` and replace `PASTE_YOUR_GROQ_API_KEY_HERE` with your Groq API key.
3. Compile and run the sample:

```powershell
javac -cp out/javask-0.1.0.jar -d out/examples examples/HelloJavAsk.java
java -cp 'out/javask-0.1.0.jar;out/examples' HelloJavAsk
```

On macOS/Linux, replace the classpath `;` with `:`. In an IDE, add the JAR as a library and import `io.github.vaibhavkatyal.javask.JavAsk`.

```java
import io.github.vaibhavkatyal.javask.JavAsk;

class Main {
    public static void main(String[] args) {
        String apiKey = "PASTE_YOUR_GROQ_API_KEY_HERE";
        String result = JavAsk.ques(apiKey, "Write a Java binary-search solution");
    }
}
```

For use without printing, or to select a model:

```java
import io.github.vaibhavkatyal.javask.GroqClient;

String apiKey = "PASTE_YOUR_GROQ_API_KEY_HERE";
String answer = new GroqClient(apiKey, "openai/gpt-oss-20b")
        .answer("Explain recursion");
```

Errors throw `GroqException`. `getStatusCode()` returns the HTTP status for API errors, or `-1` for network and parsing failures. Requests are synchronous, with 10-second connection and 60-second read timeouts. Each call consumes Groq quota.

## Maven dependency after publication

```xml
<dependency>
  <groupId>io.github.vaibhav-katyal</groupId>
  <artifactId>javask</artifactId>
  <version>0.1.0</version>
</dependency>
```

These coordinates become usable from Maven Central **only after** the owner publishes and Sonatype validates the release. The GitHub username/namespace in `pom.xml` is an assumption; replace it if it is not yours.

## Publish to Maven Central

1. Put this project in a public GitHub repository. Update the project URL, SCM, developer, license and `groupId` in `pom.xml` to match the real owner. Keep Java package paths stable after consumers start using them.
2. Register at [Sonatype Central Portal](https://central.sonatype.com/), claim and verify the matching namespace, and generate a Portal user token.
3. Install Maven and GPG, create a signing key, and make the key available to GPG. Configure `~/.m2/settings.xml` locally with a server ID of `central` and the **Portal token** username/password. Do not commit this file.
4. Run `mvn clean verify` for a signed release build with integration checks, then `mvn deploy`. The configured Central plugin uploads a bundle for validation. Review and press Publish in [Central deployments](https://central.sonatype.com/publishing/deployments). `autoPublish` is intentionally false.
5. Once published and indexed, friends can use the Maven dependency above or download the released JAR and add it to their classpath.

Each user puts their own key in their Java program. The published library JAR has no key inside it. A key placed in a public repository can be copied by others; keep your example key local. Rotate any key that has already been pasted into a chat or repository before using it again.

The default model is `openai/gpt-oss-20b`; it can be changed in `GroqClient`. Model availability can change over time.
