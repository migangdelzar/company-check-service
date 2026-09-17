# Connection Pool Tuning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Align JDBC, Redis, and provider HTTP pools with the existing throughput limits and make timeout/connection cleanup behavior explicit.

**Architecture:** Keep one pool per resource type: Spring Boot’s Hikari `DataSource` for `JdbcClient`, one shared pooled Lettuce connection factory for all distributed Redis adapters, and one shared Apache HttpClient pool for both providers. Resilience4j rate limits and bulkheads remain separate controls.

**Tech Stack:** Spring Boot 4.1.1, HikariCP, Spring Data Redis/Lettuce, Commons Pool 2, Apache HttpClient 5, Resilience4j, JUnit 5.

## Global Constraints

- Do not create a second JDBC pool; `JdbcClient` must continue using the auto-configured `DataSource`.
- Do not create separate Redis pools per adapter or provider.
- Keep provider bulkheads at 50 concurrent calls per provider and provider rate limits at 100/s.
- The distributed Redis limiter must continue failing closed when Redis is unavailable.
- Preserve the current `GET /backend-service` admission filter behavior.

---

### Task 1: Add failing tests for the tuned provider pool contract

**Files:**
- Modify: `src/test/java/com/incode/verification/adapter/out/provider/ProviderPropertiesTest.java`
- Modify: `src/main/java/com/incode/verification/adapter/out/provider/ProviderProperties.java`

- [ ] **Step 1: Extend the test fixture to construct explicit pool timeout settings** and assert they are exposed.
- [ ] **Step 2:** Run `./gradlew test --tests '*ProviderPropertiesTest'` and confirm the new API is missing or fails.
- [ ] **Step 3: Add validated duration fields to `HttpPoolProperties`: connection-request timeout, connect timeout, response timeout, validation-after-inactivity, and idle-eviction duration.
- [ ] **Step 4: Preserve the existing three-argument `ProviderProperties` constructor with safe defaults.
- [ ] **Step 5: Rerun the focused provider properties test and confirm it passes.

### Task 2: Tune the shared Apache provider HTTP client

**Files:**
- Modify: `src/main/java/com/incode/verification/configuration/ProviderHttpConfiguration.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Configure total HTTP connections to 100 and per-route connections to 50, matching the two provider bulkheads.
- [ ] **Step 2: Apply the configured connection-request, connect, and response timeouts through Apache `ConnectionConfig` and `RequestConfig`.
- [ ] **Step 3: Enable expired/idle connection eviction at the configured idle duration.
- [ ] **Step 4: Set the baseline to 100 ms pool wait, 150 ms connect, 400 ms response, 5 s validation-after-inactivity, and 30 s idle eviction.
- [ ] **Step 5: Run provider properties and provider adapter tests.

### Task 3: Tune Hikari and distributed Lettuce pools

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-distributed.yml`
- Modify: `src/test/java/com/incode/verification/configuration/RuntimeProfileConfigurationTest.java`

- [ ] **Step 1: Assert the distributed YAML contains the intended shared Lettuce settings.
- [ ] **Step 2: Set Hikari to maximum 16, minimum idle 4, connection timeout 250 ms, validation timeout 100 ms, idle timeout 10 minutes, and max lifetime 25 minutes.
- [ ] **Step 3: Set Redis command timeout to 250 ms and connect timeout to 100 ms.
- [ ] **Step 4: Set Lettuce pool max active 32, max idle 16, min idle 4, and max wait 100 ms.
- [ ] **Step 5: Run the runtime profile and persistence-related tests.

### Task 4: Final verification and review

**Files:**
- Review: all files changed by Tasks 1–3

- [ ] **Step 1: Run `./gradlew test --rerun-tasks`.
- [ ] **Step 2: Run `git diff --check`.
- [ ] **Step 3: Run `./gradlew checkstyleMain` and separate new violations from pre-existing dirty-worktree violations.
- [ ] **Step 4: Confirm no extra filter/interceptor or duplicate pool was introduced.
