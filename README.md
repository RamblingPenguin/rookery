# Rookery

Rookery is a Java 25 library for dynamic, sandboxed JVM plugin loading using the Java Platform Module System (JPMS) and the Apache Maven Resolver (Aether).

## Core Concepts

Unlike traditional flat-classpath plugin loaders, Rookery physically isolates each fetched plugin into its own discrete `ModuleLayer`. This completely eliminates dependency conflicts (`NoSuchMethodError`, `ClassNotFoundException`) between mismatched plugin transitive trees or the host application.

### Usage

**1. Include the API and Core Engine**
```xml
<dependency>
    <groupId>com.ramblingpenguin.rookery</groupId>
    <artifactId>rookery-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.ramblingpenguin.rookery</groupId>
    <artifactId>rookery-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>runtime</scope> <!-- Engine implementation injected via SPI -->
</dependency>
<dependency>
    <groupId>com.ramblingpenguin.rookery</groupId>
    <artifactId>rookery-maven</artifactId>
    <version>1.0.0-SNAPSHOT</version> <!-- Native Maven Central Plugin Fetching -->
</dependency>
```

**2. Load Plugins at Runtime**
You can use the built-in URI string parsers, environment bootstrapping (`fromEnv()`), or configuration maps (`fromConfig(Map)`). By default, `Rookery` looks for a comma-separated list of plugins under `ROOKERY_PLUGINS`.

```java
import com.ramblingpenguin.rookery.api.Rookery;
import com.ramblingpenguin.rookery.api.RookeryContext;

public class App {
    public static void main(String[] args) {
        // Automatically fetches the plugin and its entire transitive tree
        // and injects it into a shielded JPMS ModuleLayer
        try (RookeryContext context = Rookery.load("maven://com.google.guava:guava:33.1.0-jre")) {
            
            // Fetch implementations defined by the plugin's module-info.java `provides`
            context.getImplementationOf(SomeContract.class).ifPresent(plugin -> {
                plugin.execute();
            });
            
        } // AutoCloseable unloads the layer for Garbage Collection!
    }
}
```

## Features
- **Strict Isolation**: Guaranteed `ModuleLayer` sandboxing.
- **Dynamic Meta-versioning**: Safely queries `LATEST` and `RELEASE` without breaking reproducible builds (resolves strict node coordinates pre-fetch).
- **Configurable Cache**: Fine-grained snapshot update intervals via parameters (e.g., `maven://group:id:LATEST?updatePolicy=interval:15`).
- **Garbage Collection**: Calling `.close()` on a context gracefully unloads the isolated plugin from the JVM memory.

## JPMS Service Wiring (`uses` and `provides`)

Rookery relies heavily on the Java Platform Module System (JPMS) ServiceLoader mechanism to connect the host application with the isolated plugins. This is strictly enforced through your `module-info.java` files.

### 1. The Host Application (`uses`)
Your core application (the "Host") defines an interface or abstract class (e.g., `SomeContract`). In your host's `module-info.java`, you must declare that you intend to use this SPI:
```java
module my.host.app {
    requires com.ramblingpenguin.rookery.api;
    requires my.shared.api; // Where SomeContract lives

    uses my.shared.api.SomeContract; // CRITICAL: Tells JPMS you want to resolve these
}
```

### 2. The Isolated Plugin (`provides`)
The plugin you are dynamically fetching must provide an implementation of that contract. In the plugin's `module-info.java`, it declares this via the `provides` keyword:
```java
module my.custom.plugin {
    requires my.shared.api;

    // Exposes your internal class to the Host without exporting the entire package!
    provides my.shared.api.SomeContract 
        with my.custom.plugin.internal.MyContractImplementation;
}
```

When you call `Rookery.load(uri)`, Rookery spins up a protected `ModuleLayer`, scans the `provides` directives from the fetched JARs, and seamlessly connects them to the ServiceLoader `uses` declarations required by your host layer.

## Examples
Review the `examples/` directory for a full 3-module demonstration of a host application loading a Google Guava-isolated plugin entirely through `Rookery`!
