# Service rules

- Preserve application ports, DTOs, HTTP contracts, persistence schema, and provider contracts.
- Use Spring annotations at adapter and configuration boundaries; keep domain records and sealed types framework-free.
- Use JDBC/JdbcClient for persistence. Do not introduce JPA or an ORM.
- PostgreSQL is the source of truth; use Redis/Caffeine only for coordination and caching.
- Keep Resilience4j and virtual-thread configuration aligned with the existing application.
- Handle deliberate application/domain exceptions at the appropriate boundary; do not duplicate validation or exception behavior.
- Add tests before production behavior changes and preserve the existing JaCoCo gate.
- Keep Gradle build logic in the included `build-logic` convention plugins; keep dependency, plugin, formatter, contract-tool, and Java versions in `gradle/libs.versions.toml`.
- Use `fastCheck` for local unit-oriented iteration and `qualityGate` for the complete external-service, contract, and coverage gate.
