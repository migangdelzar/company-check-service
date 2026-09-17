# Verification Store and Cache AOP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify verification persistence and apply Spring cache annotations to the Caffeine L1 cache without hiding Redis leasing or PostgreSQL CAS behavior.

**Architecture:** Replace the overloaded completion service with a focused `VerificationStoreService`. Reconciliation remains a small pure application helper used by the two use-case services. `VerificationResult` becomes the application/cache result, while `VerificationResponse` remains HTTP-only. Spring's cache abstraction manages Caffeine L1; Redis remains the backing coordination store and distributed lease implementation.

**Tech Stack:** Spring Boot 4.1, Spring Cache annotations, Caffeine, Spring Data Redis, Spring transactions, Spring `RetryTemplate`, Lombok records.

## Global Constraints

- Preserve PostgreSQL claim/CAS semantics and retry-per-transaction behavior.
- Preserve Redis degraded coordination and lease takeover behavior.
- Do not use `@Cacheable` for lease acquisition or database writes.
- Keep `VerificationResponse` in the web adapter.
- Do not run tests or formatting; compile main and test sources only after changes.

---

### Task 1: Replace the application view with a result model

**Files:**
- Create: `src/main/java/com/incode/verification/application/result/VerificationResult.java`
- Modify: `src/main/java/com/incode/verification/application/port/out/CoordinationPort.java`
- Modify: `src/main/java/com/incode/verification/adapter/in/web/VerificationResponse.java`
- Modify: all production/test imports of `VerificationView`
- Delete: `src/main/java/com/incode/verification/application/port/out/VerificationView.java`

**Interfaces:**
- `VerificationResult.from(Verification)` creates the immutable application result.
- `CoordinationPort` stores and returns `VerificationResult`.
- `VerificationResponse.from(VerificationResult)` remains the HTTP mapping.

- [x] Rename the record and static projection without changing fields or JSON behavior.
- [x] Update all imports and method signatures.
- [x] Delete the old `VerificationView` type.

### Task 2: Split completion and storage responsibilities

**Files:**
- Create: `src/main/java/com/incode/verification/application/service/VerificationStoreService.java`
- Create: `src/main/java/com/incode/verification/application/service/VerificationReconciliation.java`
- Modify: `StartVerificationService.java`
- Modify: `GetVerificationService.java`
- Modify: tests and configuration constructors
- Delete: `CompleteVerificationService.java`

**Interfaces:**
- `VerificationStoreService.store(Verification, NormalizedQuery): VerificationResult` owns retry, transaction, claim/CAS, reload, and after-commit publication.
- `VerificationReconciliation.fromCached(Verification, VerificationResult): Verification` is pure.
- `VerificationReconciliation.fromShared(Verification, Verification): Verification` is pure.

- [x] Move persistence logic into `VerificationStoreService`.
- [x] Move cached/shared state transformations into the pure reconciliation helper.
- [x] Update start/get use cases to call coordination directly for reads and store only for persistence.
- [x] Preserve failure escalation only in `StartVerificationService`.

### Task 3: Enable annotation-based Caffeine L1 caching

**Files:**
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `src/main/java/com/incode/verification/adapter/config/CacheConfiguration.java`
- Modify: `src/main/java/com/incode/verification/adapter/out/coordination/RedisCoordinationAdapter.java`
- Modify: `CoordinationPort.java`

**Interfaces:**
- `RedisCoordinationAdapter.get(NormalizedQuery)` uses `@Cacheable` with the normalized query as key.
- `RedisCoordinationAdapter.put(NormalizedQuery, VerificationResult)` uses `@CachePut` and returns the stored result.
- `CacheConfiguration` exposes a `CaffeineCacheManager` with the existing dynamic TTL and maximum-size policy.

- [x] Add Spring cache support.
- [x] Enable caching in the scoped cache configuration.
- [x] Remove the manually injected raw Caffeine cache from the Redis adapter.
- [x] Keep Redis serialization/failure handling inside the adapter.
- [x] Keep lease acquisition independent from cache annotations.

### Task 4: Compile-only verification

**Files:**
- No source changes.

- [x] Run `./gradlew compileJava compileTestJava`.
- [x] Confirm `BUILD SUCCESSFUL`.
- [x] Do not run tests, `check`, or formatting.
