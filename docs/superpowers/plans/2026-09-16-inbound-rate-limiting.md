# Inbound Rate Limiting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Add a web admission limiter for `POST /backend-service`, using Resilience4j in single-node mode and Redis in distributed mode, while retaining provider-specific resilience and connection pooling.

**Architecture:** A Spring `OncePerRequestFilter` will call an injected `InboundRateLimiter` port. Profile-specific configuration supplies either a Resilience4j-backed implementation or a Redis Lua-script implementation. Provider rate limiting, bulkheads, retry/circuit-breaker behavior, UUID conflict handling, and the existing Apache HTTP pool remain unchanged.

**Tech Stack:** Spring Boot 4.1.1, Spring MVC, Resilience4j 2.4.0, Spring Data Redis/Lettuce, Redis Lua, Apache HttpClient 5.4.3, JUnit 5, Mockito, Spring MockMvc, Gradle.

## Global Constraints

- Match only `POST /backend-service` in the inbound filter.
- Use one service-wide inbound bucket; do not invent per-client identity or trust arbitrary forwarded IP headers.
- Return `429 Too Many Requests` for quota exhaustion and `Retry-After` using the configured window.
- Return `503 Service Unavailable` when the distributed Redis limiter cannot evaluate the request; never fail open.
- Keep UUID reuse/idempotency checks in `StartVerificationService`.
- Keep the existing provider limiter, provider bulkheads, retry/circuit-breaker annotations, and Apache HTTP pool.
- Do not add Redis pool tuning without load-test evidence.
- Write the failing test before production code for every behavior.
- Run `./gradlew fastCheck` for local verification and `./gradlew qualityGate` before completion when external services are available.

## File Map

Create:

- `src/main/java/com/incode/verification/application/port/out/InboundRateLimiter.java` — stable port and decision type.
- `src/main/java/com/incode/verification/adapter/in/web/InboundRateLimitFilter.java` — HTTP admission filter.
- `src/main/java/com/incode/verification/adapter/out/ratelimit/Resilience4jInboundRateLimiter.java` — single-node adapter.
- `src/main/java/com/incode/verification/adapter/out/ratelimit/RedisInboundRateLimiter.java` — distributed adapter.
- `src/main/java/com/incode/verification/configuration/InboundRateLimitConfiguration.java` — profile-specific beans.
- `src/main/java/com/incode/verification/configuration/InboundRateLimitProperties.java` — validated distributed-limit properties.
- `src/test/java/com/incode/verification/adapter/in/web/InboundRateLimitFilterTest.java` — filter behavior.
- `src/test/java/com/incode/verification/adapter/out/ratelimit/Resilience4jInboundRateLimiterTest.java` — local adapter behavior.
- `src/test/java/com/incode/verification/adapter/out/ratelimit/RedisInboundRateLimiterTest.java` — Redis adapter behavior.

Modify:

- `src/main/resources/application.yml` — common inbound limiter properties.
- `src/main/resources/application-single-node.yml` — `backendService` Resilience4j instance.
- `src/test/java/com/incode/verification/configuration/RuntimeProfileConfigurationTest.java` — profile wiring assertions.
- `src/test/java/com/incode/verification/adapter/in/web/BackendServiceControllerTest.java` — rejected requests do not reach the use case.
- `src/main/java/com/incode/verification/adapter/out/provider/ProviderProperties.java` — validated provider HTTP pool settings.
- `src/main/java/com/incode/verification/configuration/ProviderHttpConfiguration.java` — consume configured pool values.
- `src/main/resources/application.yml` — Hikari and provider pool settings.
- `src/main/resources/application-distributed.yml` — Redis Lettuce pool settings.
- `gradle/libs.versions.toml` and `build.gradle.kts` — Commons Pool 2 dependency for Lettuce pooling.
- `src/test/java/com/incode/verification/adapter/out/provider/ProviderPropertiesTest.java` — pool validation coverage.

## Task 1: Define the inbound limiter port and decision model

**Files:**

- Create: `src/main/java/com/incode/verification/application/port/out/InboundRateLimiter.java`
- Test: `src/test/java/com/incode/verification/adapter/in/web/InboundRateLimitFilterTest.java`

**Interface:**

```java
public interface InboundRateLimiter {
  Decision tryAcquire();

  record Decision(Status status, Duration retryAfter) {
    public enum Status { ALLOWED, REJECTED, UNAVAILABLE }

    public boolean allowed() {
      return status == Status.ALLOWED;
    }
  }
}
```

- [ ] **Step 1: Write the failing consumer test** in the filter test using a fake `InboundRateLimiter` that returns each decision.
- [ ] **Step 2: Run the focused test** with `./gradlew test --tests '*InboundRateLimitFilterTest'`; expect compilation failure because the port/filter do not exist.
- [ ] **Step 3: Add the minimal port and immutable decision model.** Normalize negative retry durations to zero in the record constructor.
- [ ] **Step 4: Run the focused test** and confirm it still fails only on missing filter behavior.
- [ ] **Step 5: Commit:** `git add src/main/java/com/incode/verification/application/port/out/InboundRateLimiter.java src/test/java/com/incode/verification/adapter/in/web/InboundRateLimitFilterTest.java && git commit -m "feat(rate-limit): define inbound limiter port"`.

## Task 2: Implement and test the web admission filter

**Files:**

- Create: `src/main/java/com/incode/verification/adapter/in/web/InboundRateLimitFilter.java`
- Create: `src/test/java/com/incode/verification/adapter/in/web/InboundRateLimitFilterTest.java`
- Modify: `src/test/java/com/incode/verification/adapter/in/web/BackendServiceControllerTest.java`

**Behavior:**

```java
final class InboundRateLimitFilter extends OncePerRequestFilter {
  private final InboundRateLimiter limiter;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !("POST".equals(request.getMethod())
        && "/backend-service".equals(request.getRequestURI()));
  }
}
```

- Allowed decision: call `filterChain.doFilter`.
- Rejected decision: status `429`, `Content-Type: application/problem+json` or the project’s existing error content type, `Retry-After` in seconds, and no chain invocation.
- Unavailable decision: status `503`, `Retry-After` when available, and no chain invocation.
- Do not parse `verificationId`, query parameters, or forwarded headers.
- Register the filter as a Spring bean so its limiter dependency is injected and it is auto-registered by Boot.

- [ ] **Step 1: Write tests** for allowed `POST /backend-service`, rejected `POST /backend-service`, unavailable limiter, non-matching method/path, and controller/use-case non-invocation after rejection.
- [ ] **Step 2: Run:** `./gradlew test --tests '*InboundRateLimitFilterTest' --tests '*BackendServiceControllerTest'`; expect red tests.
- [ ] **Step 3: Implement** the filter with `OncePerRequestFilter`, exact path/method matching, status mapping, and integer-ceiling `Retry-After`.
- [ ] **Step 4: Run the focused tests** and confirm green.
- [ ] **Step 5: Run `./gradlew fastCheck`** to catch Spring registration and style issues.
- [ ] **Step 6: Commit:** `git add src/main/java/com/incode/verification/adapter/in/web/InboundRateLimitFilter.java src/test/java/com/incode/verification/adapter/in/web/InboundRateLimitFilterTest.java src/test/java/com/incode/verification/adapter/in/web/BackendServiceControllerTest.java && git commit -m "feat(rate-limit): add backend service admission filter"`.

## Task 3: Add the single-node Resilience4j implementation

**Files:**

- Create: `src/main/java/com/incode/verification/adapter/out/ratelimit/Resilience4jInboundRateLimiter.java`
- Create: `src/test/java/com/incode/verification/adapter/out/ratelimit/Resilience4jInboundRateLimiterTest.java`
- Modify: `src/main/resources/application-single-node.yml`

**Behavior:**

- Expose a Spring bean implementing `InboundRateLimiter`.
- Receive the Resilience4j `RateLimiterRegistry`-managed `RateLimiter` for the `backendService` instance.
- Call `acquirePermission()` without waiting; return `ALLOWED` when it succeeds.
- Return `REJECTED` with the configured refresh duration when `acquirePermission()` returns false.
- Configure `backendService` with `limit-for-period`, `limit-refresh-period`, and `timeout-duration: 0`; it must never wait for a permit.

- [ ] **Step 1: Write the failing adapter/configuration tests** for permit acceptance, immediate rejection after the limit, and zero wait duration.
- [ ] **Step 2: Run:** `./gradlew test --tests '*Resilience4jInboundRateLimiterTest'`; expect red tests.
- [ ] **Step 3: Implement** the adapter and add the `backendService` Resilience4j YAML instance.
- [ ] **Step 4: Run the focused tests** and confirm green.
- [ ] **Step 5: Commit:** `git add src/main/java/com/incode/verification/adapter/out/ratelimit/Resilience4jInboundRateLimiter.java src/test/java/com/incode/verification/adapter/out/ratelimit/Resilience4jInboundRateLimiterTest.java src/main/resources/application-single-node.yml && git commit -m "feat(rate-limit): add single-node inbound limiter"`.

## Task 4: Add the distributed Redis implementation

**Files:**

- Create: `src/main/java/com/incode/verification/adapter/out/ratelimit/RedisInboundRateLimiter.java`
- Create: `src/test/java/com/incode/verification/adapter/out/ratelimit/RedisInboundRateLimiterTest.java`
- Create: `src/main/java/com/incode/verification/configuration/InboundRateLimitProperties.java`
- Modify: `src/main/resources/application.yml`

**Behavior:**

- Use one fixed-window Redis key such as `<key-prefix>backend-service`.
- Use one atomic Lua script that increments the counter, sets the expiry on the first request, and returns allow/reject plus remaining window information.
- Return `ALLOWED` when the count is within the configured limit.
- Return `REJECTED` with a positive retry duration when the limit is exceeded.
- Catch Redis/runtime failures and return `UNAVAILABLE`; do not allow the request through.
- Validate positive limit, positive refresh duration, and nonblank key prefix through `@ConfigurationProperties` and `@Validated`.

- [ ] **Step 1: Write tests** for first permit, limit boundary, rejection, key/arguments passed to the script, and Redis failure returning `UNAVAILABLE`.
- [ ] **Step 2: Run:** `./gradlew test --tests '*RedisInboundRateLimiterTest'`; expect red tests.
- [ ] **Step 3: Implement** the properties record and Redis adapter using `StringRedisTemplate` and `DefaultRedisScript`.
- [ ] **Step 4: Run the focused tests** and confirm green.
- [ ] **Step 5: Run the existing Redis limiter tests** to ensure no provider behavior changed.
- [ ] **Step 6: Commit:** `git add src/main/java/com/incode/verification/adapter/out/ratelimit/RedisInboundRateLimiter.java src/main/java/com/incode/verification/configuration/InboundRateLimitProperties.java src/test/java/com/incode/verification/adapter/out/ratelimit/RedisInboundRateLimiterTest.java src/main/resources/application.yml && git commit -m "feat(rate-limit): add distributed inbound limiter"`.

## Task 5: Wire profile-specific beans and configuration

**Files:**

- Create: `src/main/java/com/incode/verification/configuration/InboundRateLimitConfiguration.java`
- Modify: `src/test/java/com/incode/verification/configuration/RuntimeProfileConfigurationTest.java`

**Wiring:**

```java
@Configuration(proxyBeanMethods = false)
public class InboundRateLimitConfiguration {
  @Bean
  @Profile("single-node")
  InboundRateLimiter singleNodeInboundRateLimiter(
      Resilience4jInboundRateLimiter limiter) {
    return limiter;
  }

  @Bean
  @Profile("distributed")
  InboundRateLimiter distributedInboundRateLimiter(
      RedisInboundRateLimiter limiter) {
    return limiter;
  }
}
```

- Exactly one `InboundRateLimiter` bean must exist in each supported profile.
- Single-node must not require Redis.
- Distributed must use `RedisInboundRateLimiter` and the configured inbound key prefix.
- The existing `ProviderRateLimiter` profile wiring must remain unchanged.

- [ ] **Step 1: Write failing profile tests** asserting the implementation type in each profile and absence of the opposite implementation.
- [ ] **Step 2: Run:** `./gradlew test --tests '*RuntimeProfileConfigurationTest'`; expect red tests.
- [ ] **Step 3: Implement** the configuration class and property binding.
- [ ] **Step 4: Run the profile tests** and confirm green.
- [ ] **Step 5: Run `./gradlew fastCheck`** and fix only feature-related failures.
- [ ] **Step 6: Commit:** `git add src/main/java/com/incode/verification/configuration/InboundRateLimitConfiguration.java src/test/java/com/incode/verification/configuration/RuntimeProfileConfigurationTest.java && git commit -m "feat(rate-limit): wire inbound limiter profiles"`.

## Task 6: Verify end-to-end behavior and preserve resource controls

**Files:**

- No new production files; existing tests from Tasks 2–5 remain the verification surface.

- [ ] **Step 1: Confirm the integration/slice assertions** that a rejected request does not invoke `StartVerificationUseCase` and an allowed request preserves the existing response/no-store behavior.
- [ ] **Step 2: Run focused web and configuration tests:** `./gradlew test --tests '*InboundRateLimit*' --tests '*BackendServiceControllerTest' --tests '*RuntimeProfileConfigurationTest'`.
- [ ] **Step 3: Run `./gradlew fastCheck` and record the result.**
- [ ] **Step 4: Run `./gradlew qualityGate` when PostgreSQL/Redis/provider test dependencies are available; otherwise report the exact blocked task and output.
- [ ] **Step 5: Inspect the final diff** to verify no provider resilience or connection-pool settings changed.
- [ ] **Step 6: Do not create a separate commit; the focused tests and configuration changes are committed with their owning tasks.

## Task 7: Make connection-pool configuration explicit

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `src/main/java/com/incode/verification/adapter/out/provider/ProviderProperties.java`
- Modify: `src/main/java/com/incode/verification/configuration/ProviderHttpConfiguration.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-distributed.yml`
- Modify: `src/test/java/com/incode/verification/adapter/out/provider/ProviderPropertiesTest.java`

**Behavior:**

- Keep `spring.datasource.hikari.*` as the JDBC pool configuration consumed by the `DataSource` passed into `JdbcClient`; do not create a second JDBC pool.
- Add a validated provider pool record with `max-total` and `max-per-route`, preserving defaults of 100 and 100 for existing behavior.
- Replace the hard-coded Apache pool values with the validated provider pool properties.
- Add `org.apache.commons:commons-pool2` through the version catalog.
- In `application-distributed.yml`, enable one shared Lettuce pool with explicit `max-active`, `max-idle`, `min-idle`, and `max-wait` values. Single-node must continue excluding Redis auto-configuration.
- Do not add an MVC interceptor for pool management; pool ownership remains in the DataSource, Lettuce factory, and provider HTTP client configuration.

- [ ] **Step 1: Write failing property tests** for invalid zero/negative provider pool sizes and valid pool settings.
- [ ] **Step 2: Run:** `./gradlew test --tests '*ProviderPropertiesTest'`; expect red tests for the new record/validation.
- [ ] **Step 3: Implement** the nested provider pool properties, YAML bindings, and Apache pool wiring.
- [ ] **Step 4: Add the Commons Pool 2 dependency and distributed Lettuce pool properties.**
- [ ] **Step 5: Run the focused property and configuration tests** and confirm green.
- [ ] **Step 6: Run `./gradlew fastCheck`** and verify the existing `JdbcClient`/Hikari wiring remains unchanged.
- [ ] **Step 7: Commit:** `git add gradle/libs.versions.toml build.gradle.kts src/main/java/com/incode/verification/adapter/out/provider/ProviderProperties.java src/main/java/com/incode/verification/configuration/ProviderHttpConfiguration.java src/main/resources/application.yml src/main/resources/application-distributed.yml src/test/java/com/incode/verification/adapter/out/provider/ProviderPropertiesTest.java && git commit -m "feat(config): configure connection pools"`.

## Definition of Done

- [ ] `POST /backend-service` is rate limited before controller/use-case execution.
- [ ] Single-node uses Resilience4j `backendService` limiter.
- [ ] Distributed uses Redis atomic limiting and fails closed on Redis outage.
- [ ] Rejections return `429` with `Retry-After`; protection dependency outage returns `503`.
- [ ] Provider-specific rate limiting remains active in both profiles.
- [ ] Provider bulkheads and the existing HTTP connection pool remain unchanged.
- [ ] UUID reuse/idempotency behavior remains unchanged.
- [ ] Focused tests and `fastCheck` pass; `qualityGate` is run or its blocker is documented.
- [ ] No unrelated user changes are staged or committed.
