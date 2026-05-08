# 🛡️ Schema Guard Spring Boot Starter

**Shift-Left Data Integrity for Apache Kafka**

Schema Guard is a production-grade Spring Boot Starter designed to prevent "Poison Pills" from ever reaching your Kafka brokers. By intercepting messages at the producer level and validating them against a remote JSON Schema Registry, it ensures that only high-quality, valid data enters your event streams.

---

## 🚀 Why Schema Guard?

In distributed systems, an invalid message (Poison Pill) can crash entire consumer groups, leading to massive backlogs and manual cleanup efforts. Schema Guard solves this by enforcing validation **synchronously** on the producer side.

- **✅ Synchronous Blocking**: Uses a Java Dynamic Proxy over the Kafka Producer to throw `SchemaValidationException` immediately on the calling thread if validation fails.
- **🛡️ Fail-Open Strategy**: Integrated with **Resilience4j Circuit Breaker**. If your Schema Registry is down, the system automatically fails-open to ensure business continuity while logging warnings.
- **⚡ High Performance**: Powered by **Caffeine Caching** to minimize network round-trips to the registry.
- **🧩 Plug-and-Play**: Pure Spring Boot Auto-Configuration. Zero code changes required in your existing services.

---

## 📦 Installation

### Maven
Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.schemaguard</groupId>
    <artifactId>schema-guard-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### GitHub Packages Repository (Required)
This artifact is published to GitHub Packages (not Maven Central). Add the GitHub Packages Maven repository:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/nithinraaj27/kafka-schema-guard</url>
  </repository>
</repositories>
```

Then authenticate Maven by adding this to `~/.m2/settings.xml` (a GitHub token is required; scope `read:packages`):

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

---

## ⚙️ Configuration

Enable validation and map your Kafka topics to their respective Schema IDs in your `application.properties`:

```properties
# Enable the guard
schema-guard.enabled=true

# Registry Configuration
schema-guard.registry-url=https://api.your-registry.com/schemas
schema-guard.cache-ttl-minutes=60

# Topic-to-Schema Mapping
schema-guard.topics.testing-events=your-schema-v1
schema-guard.topics.order-events=order-schema-v2
```

---

## 🧪 Live Tester UI

We provide a clean, modern web interface to test your schemas and payloads locally before deploying.

### How to Start the UI:
1. Navigate to the UI directory:
   ```bash
   cd tester-ui
   ```
2. Install dependencies and start the dev server:
   ```bash
   npm install
   npm run dev
   ```
3. Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 🛠️ Development & Testing

### Running Integration Tests
The project includes a robust test suite using **EmbeddedKafka** and **WireMock** to simulate the full lifecycle of a validated message.

```bash
mvn test
```

### Publishing to GitHub Packages (Maintainers)
To publish a new version:

1. Create a GitHub token with `write:packages` + `read:packages`.
2. Export the token and deploy:

```bash
export GITHUB_TOKEN="YOUR_GITHUB_TOKEN"
mvn -DskipTests deploy
```

### Expressive Errors
When validation fails, Schema Guard provides strictly typed errors:
- `MissingField`: Identifies the exact property missing from the JSON.
- `SchemaValidationError`: Provides a detailed path and description of the type mismatch.

---

## 🛡️ License
Distributed under the MIT License. See `LICENSE` for more information.
