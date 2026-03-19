# AI Engineering Guidelines (AGENTS)

When interacting as an AI agent within the `Rookery` codebase, you MUST adhere to the following strict architectural guidelines:

## 1. SPI Boundary Strictness
- `rookery-api` MUST NOT depend on `rookery-core`. The core engine is discovered at runtime via `RookeryEngine` ServiceLoader SPI injection.
- When creating host applications, ensure `rookery-core` is specified with `<scope>runtime</scope>`. Do not compile heavily against internal classes.

## 2. Dynamic Fetching Strategies
- Use `ArtifactLocator<T>` generically. Do not hardcode specific String parsers originally intended for specific repositories.
- All environment loading features (`fromEnv()`, `fromConfig(Map)`) operate seamlessly by routing input URIs to the loaded ServiceLoader implementations of `ArtifactLocator.parseURI()`.

## 3. Sandboxed Executions
- When building `examples/` or writing tests, avoid mixing plugin transitive dependencies in the host `pom.xml`. The primary goal of `Rookery` is to prove complete classpath extraction.

## 4. Maven Central Configuration
- Rookery publishes directly to Sonatype. Any new parent configuration updates or profile changes MUST preserve the execution bindings for `maven-source-plugin` and `maven-javadoc-plugin` attachment schemas, or the CI deployment will be forcefully rejected.
