# Simplify Backend to Layered Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Replace the Java backend's DDD/hexagonal package structure with the approved conventional layered structure while preserving every public contract and runtime behavior.

**Architecture:** Use controller, service, repository, client, mapper, exception, and config layers. Keep only packages required by current behavior; do not create empty security, OpenAPI, async, event, projection, aspect, util, or constant packages. Business records move to service/model as simple records rather than domain aggregates.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring MVC, RestClient, JDBC, Redis, Resilience4j, Jackson, ArchUnit, GraalVM Native Build Tools, Gradle.

## Global Constraints

- Migrate only company-check-service; company-check-provider remains unchanged.
- Preserve existing HTTP API, provider contracts, persistence behavior, resilience behavior, coordination behavior, scheduled expiration, observability, and native-image support.
- FREE remains the primary provider; PREMIUM is used for 503/transient/malformed/empty FREE results.
- Inactive companies are excluded before the backend response is created; additional active matches remain in otherResults.
- Provider response records never leave client/dto.
- Controllers never call repositories or clients directly.
- Services do not depend on Spring MVC, JDBC, Redis, or provider wire DTOs.
- Existing unrelated working-tree changes belong to the user and must be preserved.
- Every behavior change follows RED → GREEN → REFACTOR.
- Every completed task ends with its focused tests and a small commit containing only that task's files.

---

## File and responsibility map

| Target path | Responsibility |
| --- | --- |
| src/main/java/com/incode/verification/controller | HTTP controllers, filters, and API DTOs |
| src/main/java/com/incode/verification/service | Business workflows and service models |
| src/main/java/com/incode/verification/repository | Repository contracts and persistence/coordination implementations |
| src/main/java/com/incode/verification/client | FREE/PREMIUM HTTP clients and provider DTOs |
| src/main/java/com/incode/verification/mapper | Explicit API/entity/provider/state conversions |
| src/main/java/com/incode/verification/exception | Business, integration, and HTTP error handling |
| src/main/java/com/incode/verification/config | Spring beans, properties, profiles, and native hints |
| src/test/java/com/incode/verification/architecture | Layered dependency rules |

The existing source files are moved, not copied. The final cleanup removes obsolete adapter, application, domain, and port packages.

## Task 1: Finish typed provider clients and native-image support

**Files:**

- Create: src/main/java/com/incode/verification/client/ProviderClient.java
- Create: src/main/java/com/incode/verification/client/FreeProviderClient.java
- Create: src/main/java/com/incode/verification/client/PremiumProviderClient.java
- Create: src/main/java/com/incode/verification/client/ProviderClientSupport.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/dto/FreeProviderCompanyDto.java to client/dto/FreeCompanyResponse.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/dto/PremiumProviderCompanyDto.java to client/dto/PremiumCompanyResponse.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/ProviderResponseMapper.java to mapper/ProviderMapper.java
- Move: src/main/java/com/incode/verification/configuration/provider/ProviderRuntimeHints.java to config/ProviderRuntimeHints.java
- Modify: src/main/java/com/incode/verification/configuration/provider/ProviderHttpConfiguration.java
- Modify: src/main/java/com/incode/verification/configuration/provider/ProviderResilienceConfiguration.java
- Modify: src/main/java/com/incode/verification/configuration/provider/DistributedProviderResilienceConfiguration.java
- Test: src/test/java/com/incode/verification/mapper/ProviderMapperTest.java
- Test: src/test/java/com/incode/verification/config/ProviderRuntimeHintsTest.java
- Test: src/test/java/com/incode/verification/client/TypedProviderClientTest.java

**Interfaces:**

- ProviderClient exposes ProviderResult lookup(NormalizedQuery query).
- FreeProviderClient and PremiumProviderClient each accept RestClient and ProviderEndpointProperties.
- ProviderMapper exposes mapFree(FreeCompanyResponse[] payload) and mapPremium(PremiumCompanyResponse[] payload).
- TypedProviderClient<T> keeps the transport generic while each concrete client supplies its response record and mapper.
- ProviderRuntimeHints registers both response records for constructors, public methods, and declared field access.

- [x] **Step 1: Write failing typed DTO and hints tests.**

Use provider payloads with registration_date/is_active for FREE and companyFullAddress/isActive for PREMIUM. Assert that legacy fullAddress is rejected by the premium conversion, empty arrays become empty results, null payloads are rejected, and both response records have RuntimeHints reflection entries.

- [x] **Step 2: Run the focused tests and verify RED.**

Run:

    ./gradlew test --tests com.incode.verification.client.ProviderMapperTest --tests com.incode.verification.config.ProviderRuntimeHintsTest --no-daemon --console=plain

Expected: compilation or assertion failure because the target packages and client classes do not exist yet.

- [x] **Step 3: Move the DTOs and implement typed conversion.**

Use response records with JsonProperty for registration_date, is_active, and isActive. Convert LocalDate and validate every required field before constructing service/model.Company. Do not use JsonNode or provider field-name conditionals.

- [x] **Step 4: Implement the client interface and two concrete clients.**

Each client performs GET against its configured path with the same query URI variable and API key. ProviderClientSupport contains only shared HTTP exception classification. Keep FREE/PREMIUM resilience annotations on their respective clients.

- [x] **Step 5: Register the clients and hints in config.**

Update both single-node and distributed configuration classes to expose beans named freeProvider and premiumProvider. Attach ImportRuntimeHints to the provider HTTP configuration.

- [x] **Step 6: Run the focused tests and verify GREEN.**

Run the command from Step 2. Expected: all provider DTO, client, and hints tests pass.

- [x] **Step 7: Commit the provider client slice.**

    git add src/main/java/com/incode/verification/client src/main/java/com/incode/verification/mapper/ProviderMapper.java src/main/java/com/incode/verification/config src/test/java/com/incode/verification/client src/test/java/com/incode/verification/config/ProviderRuntimeHintsTest.java
    git commit -m "refactor: use typed layered provider clients"

## Task 2: Move business models into the service layer

**Files:**

- Move: src/main/java/com/incode/verification/domain/company/Company.java to service/model/Company.java
- Move: src/main/java/com/incode/verification/domain/identity/UuidV7.java to util/UuidV7.java
- Move: src/main/java/com/incode/verification/domain/provider/ProviderFailure.java to service/model/ProviderFailure.java
- Move: src/main/java/com/incode/verification/domain/provider/ProviderResult.java to service/model/ProviderResult.java
- Move: src/main/java/com/incode/verification/domain/provider/ProviderType.java to service/model/ProviderType.java
- Move: src/main/java/com/incode/verification/domain/query/NormalizedQuery.java to service/model/NormalizedQuery.java
- Move: src/main/java/com/incode/verification/domain/query/InvalidQueryException.java to exception/domain/InvalidQueryException.java
- Move: src/main/java/com/incode/verification/domain/verification/Verification.java to service/model/Verification.java
- Move: src/main/java/com/incode/verification/domain/verification/VerificationState.java to service/model/VerificationState.java
- Move: src/main/java/com/incode/verification/domain/verification/VerificationStatus.java to service/model/VerificationStatus.java
- Move: src/main/java/com/incode/verification/application/result/VerificationResult.java to service/model/VerificationResult.java
- Move: src/main/java/com/incode/verification/application/port/in/StartVerificationCommand.java to service/model/StartVerificationCommand.java
- Modify: all production and test imports referencing com.incode.verification.domain or application.result
- Test: src/test/java/com/incode/verification/service/model/VerificationModelTest.java

**Interfaces:**

- Keep record constructors and accessors stable during this move.
- Keep VerificationResult.from(Verification) stable until controller DTOs are introduced.
- Keep NormalizedQuery.normalize(String) stable.

- [x] **Step 1: Add package-level model tests for normalization, active filtering, and otherResults.**

Move the existing VerificationTest assertions into service/model/VerificationModelTest and add an empty-result assertion for ProviderResult.

- [x] **Step 2: Run the model tests and verify RED because imports still point to old packages.**

    ./gradlew test --tests com.incode.verification.service.model.VerificationModelTest --no-daemon --console=plain

- [x] **Step 3: Move the records and update imports mechanically.**

Use git mv for each source/test package. Keep the records simple; do not add Spring annotations, persistence annotations, or provider DTO imports.

- [x] **Step 4: Run model and existing application tests.**

    ./gradlew test --tests com.incode.verification.service.model.VerificationModelTest --tests com.incode.verification.application.VerificationUseCaseServiceTest --no-daemon --console=plain

Expected: all moved-model behavior passes with no old domain imports in production.

- [x] **Step 5: Commit the service-model slice.**

    git add src/main/java/com/incode/verification/service src/main/java/com/incode/verification/util src/main/java/com/incode/verification/exception/domain src/test/java/com/incode/verification/service
    git commit -m "refactor: move business models into service layer"

## Task 3: Simplify provider orchestration into ProviderService

**Files:**

- Create: src/main/java/com/incode/verification/service/ProviderService.java
- Move: src/main/java/com/incode/verification/application/service/ProviderResolutionService.java to service/ProviderService.java
- Move: src/main/java/com/incode/verification/domain/provider/FallbackPolicy.java logic into service/ProviderService.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/ProviderContractException.java to exception/ProviderContractException.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/ProviderTransientException.java to exception/ProviderTransientException.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/ProviderRateLimitExceededException.java to exception/ProviderRateLimitExceededException.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/FreeProvider.java to client/FreeProviderClient.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/PremiumProvider.java to client/PremiumProviderClient.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/DistributedFreeProvider.java to client/DistributedFreeProviderClient.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/DistributedPremiumProvider.java to client/DistributedPremiumProviderClient.java
- Move: src/main/java/com/incode/verification/adapter/out/provider/ResilientProvider.java to client/ProviderClientSupport.java
- Test: src/test/java/com/incode/verification/service/ProviderServiceTest.java

**Interfaces:**

- ProviderService exposes ProviderResult resolve(NormalizedQuery query).
- ProviderService calls the FREE ProviderClient first and PREMIUM only when the FREE result is a failure requiring fallback or a successful empty result.

- [x] **Step 1: Add failing ProviderService tests for failure and empty-result fallback.**

Use lambdas or small fakes implementing ProviderClient. Assert call order [free, premium], no premium call for a successful non-empty FREE result, and no fallback for a client error.

- [x] **Step 2: Run the tests and verify RED.**

    ./gradlew test --tests com.incode.verification.service.ProviderServiceTest --no-daemon --console=plain

- [x] **Step 3: Implement ProviderService with explicit fallback logic.**

Keep the rule visible in one method:

    var freeResult = free.lookup(query);
    if (freeResult instanceof ProviderResult.Success success && !success.companies().isEmpty()) return freeResult;
    if (!requiresFallback(freeResult)) return freeResult;
    return premium.lookup(query);

Use the service model types and preserve existing failure classifications.

- [x] **Step 4: Run provider and application tests.**

    ./gradlew test --tests com.incode.verification.service.ProviderServiceTest --tests com.incode.verification.application.VerificationUseCaseServiceTest --no-daemon --console=plain

- [x] **Step 5: Commit the provider orchestration slice.**

    git add src/main/java/com/incode/verification/service/ProviderService.java src/main/java/com/incode/verification/client src/main/java/com/incode/verification/exception src/test/java/com/incode/verification/service/ProviderServiceTest.java
    git commit -m "refactor: simplify provider orchestration"

## Task 4: Move and split controller contracts

**Files:**

- Move: src/main/java/com/incode/verification/adapter/in/web/BackendServiceController.java to controller/BackendServiceController.java
- Move: src/main/java/com/incode/verification/adapter/in/web/VerificationController.java to controller/VerificationController.java
- Move: src/main/java/com/incode/verification/adapter/in/web/BackendServiceRequest.java to controller/dto/request/BackendServiceRequest.java
- Move: src/main/java/com/incode/verification/adapter/in/web/CompanyResponse.java to controller/dto/response/CompanyResponse.java
- Move: src/main/java/com/incode/verification/adapter/in/web/VerificationResponse.java to controller/dto/response/VerificationResponse.java
- Move: src/main/java/com/incode/verification/adapter/in/web/InboundRateLimitFilter.java to controller/InboundRateLimitFilter.java
- Move: src/main/java/com/incode/verification/adapter/in/scheduling/VerificationExpirationScheduler.java to controller/VerificationExpirationScheduler.java
- Create: src/main/java/com/incode/verification/mapper/VerificationMapper.java
- Test: src/test/java/com/incode/verification/controller/BackendServiceControllerTest.java
- Test: src/test/java/com/incode/verification/controller/VerificationControllerTest.java
- Test: src/test/java/com/incode/verification/controller/VerificationResponseJsonSliceTest.java
- Test: src/test/java/com/incode/verification/controller/InboundRateLimitFilterTest.java
- Test: src/test/java/com/incode/verification/controller/VerificationExpirationSchedulerTest.java

**Interfaces:**

- BackendServiceController receives BackendServiceRequest and returns VerificationResponse.
- VerificationMapper maps service/model.VerificationResult into response DTOs without exposing service models to Jackson.
- Controllers depend on service.VerificationService and service.ExpirationService only.

- [x] **Step 1: Move tests and update their package declarations before production moves.**

Run the focused controller suite and verify RED from missing target classes/imports.

- [x] **Step 2: Move request/response records into request and response packages.**

Preserve every JSON property, validation annotation, endpoint path, HTTP method, and error response field.

- [x] **Step 3: Add VerificationMapper and replace direct model serialization.**

Map company, otherResults, provider, failure, and status explicitly. Keep inactive filtering in service/model workflow, not in the controller.

- [x] **Step 4: Update controllers and scheduler to call services.**

Do not inject repositories, clients, or configuration properties into controllers.

- [x] **Step 5: Run the controller suite and verify GREEN.**

    ./gradlew test --tests com.incode.verification.controller.* --no-daemon --console=plain

- [x] **Step 6: Commit the controller slice.**

    git add src/main/java/com/incode/verification/controller src/main/java/com/incode/verification/mapper/VerificationMapper.java src/test/java/com/incode/verification/controller
    git commit -m "refactor: simplify controller layer"

## Task 5: Move service workflows and consolidate use cases

**Files:**

- Create: src/main/java/com/incode/verification/service/VerificationService.java
- Create: src/main/java/com/incode/verification/service/ExpirationService.java
- Move: src/main/java/com/incode/verification/application/service/StartVerificationService.java to service/VerificationService.java
- Move: src/main/java/com/incode/verification/application/service/GetVerificationService.java behavior into service/VerificationService.java
- Move: src/main/java/com/incode/verification/application/service/ExpireVerificationsService.java to service/ExpirationService.java
- Move: src/main/java/com/incode/verification/application/service/VerificationRecoveryService.java to service/VerificationRecoveryService.java
- Move: src/main/java/com/incode/verification/application/service/VerificationStoreService.java to service/VerificationStoreService.java
- Move: src/main/java/com/incode/verification/application/service/VerificationReconciliation.java to mapper/VerificationReconciliationMapper.java
- Remove: src/main/java/com/incode/verification/application/port/in/StartVerificationUseCase.java
- Remove: src/main/java/com/incode/verification/application/port/in/GetVerificationUseCase.java
- Remove: src/main/java/com/incode/verification/application/port/in/ExpireVerificationsUseCase.java
- Test: src/test/java/com/incode/verification/service/VerificationServiceTest.java
- Test: src/test/java/com/incode/verification/service/ExpirationServiceTest.java

**Interfaces:**

- VerificationService.start(StartVerificationCommand command) returns service/model.VerificationResult.
- VerificationService.get(UUID verificationId) returns service/model.VerificationResult.
- ExpirationService.expire(Instant now, int batchSize) returns int.
- Recovery and storage remain separate services because they encapsulate cache/transaction coordination and are independently tested.

- [x] **Step 1: Add service tests for start, get, duplicate IDs, shared cache, provider failures, and terminal replay.**

Move the behavior currently covered by VerificationUseCaseServiceTest into VerificationServiceTest and preserve its repository/coordination fakes.

- [x] **Step 2: Run the service tests and verify RED because target service classes do not exist.**

    ./gradlew test --tests com.incode.verification.service.VerificationServiceTest --tests com.incode.verification.service.ExpirationServiceTest --no-daemon --console=plain

- [x] **Step 3: Consolidate start and get workflows in VerificationService.**

Keep transaction boundaries and coordination acquisition in the service. Inject ProviderService, repository contracts, CoordinationService, VerificationStoreService, VerificationRecoveryService, Clock, and verification lifetime.

- [x] **Step 4: Move expiration into ExpirationService and update the scheduler.**

Retain positive batch-size validation and existing scheduler timing/profile behavior.

- [x] **Step 5: Run service, controller, and recovery tests.**

    ./gradlew test --tests com.incode.verification.service.* --tests com.incode.verification.controller.* --no-daemon --console=plain

- [x] **Step 6: Commit the service slice.**

    git add src/main/java/com/incode/verification/service src/main/java/com/incode/verification/mapper/VerificationReconciliationMapper.java src/main/java/com/incode/verification/controller/VerificationExpirationScheduler.java src/test/java/com/incode/verification/service
    git commit -m "refactor: consolidate verification services"

## Task 6: Move persistence, coordination, and rate limiting

**Files:**

- Move: src/main/java/com/incode/verification/application/port/out/VerificationRepository.java to repository/VerificationRepository.java
- Move: src/main/java/com/incode/verification/adapter/out/persistence/JdbcVerificationRepository.java to repository/JdbcVerificationRepository.java
- Move: src/main/java/com/incode/verification/adapter/out/persistence/VerificationEntity.java to repository/entity/VerificationEntity.java
- Move: src/main/java/com/incode/verification/adapter/out/persistence/VerificationStateCodec.java to mapper/VerificationStateMapper.java
- Move: src/main/java/com/incode/verification/application/port/out/CoordinationPort.java to repository/CoordinationRepository.java
- Move: src/main/java/com/incode/verification/application/port/out/ExpirationLock.java to repository/ExpirationLock.java
- Move: src/main/java/com/incode/verification/adapter/out/coordination/* to repository/coordination/*
- Move: src/main/java/com/incode/verification/application/port/out/InboundRateLimiter.java to repository/InboundRateLimiter.java
- Move: src/main/java/com/incode/verification/application/port/out/ProviderLookupPort.java to client/ProviderClient.java
- Move: src/main/java/com/incode/verification/adapter/out/ratelimit/* to repository/ratelimit/*
- Test: src/test/java/com/incode/verification/repository/*
- Test: src/test/java/com/incode/verification/mapper/VerificationStateMapperTest.java

**Interfaces:**

- Repository interfaces remain simple Java contracts with no web or provider DTOs.
- JdbcVerificationRepository still performs explicit column selection and atomic claim/complete operations.
- CoordinationRepository keeps get, put, acquire, and lease behavior used by services.

- [x] **Step 1: Move repository tests and rename package declarations.**

Preserve all Redis, JDBC, expiration-lock, and rate-limit assertions. Run them before moving production code to capture the RED baseline.

- [x] **Step 2: Move persistence entities and repository implementations.**

Keep SQL text and transaction semantics unchanged. Update VerificationStateMapper imports to service/model types.

- [x] **Step 3: Move coordination and rate-limit implementations.**

Keep local/distributed profiles, Redis keys, lease behavior, and provider/inbound limiter semantics unchanged.

- [x] **Step 4: Update services and config to inject repository contracts.**

No service may import repository/entity or client/dto.

- [x] **Step 5: Run repository and service tests.**

    ./gradlew test --tests com.incode.verification.repository.* --tests com.incode.verification.service.* --no-daemon --console=plain

- [x] **Step 6: Commit the repository slice.**

    git add src/main/java/com/incode/verification/repository src/main/java/com/incode/verification/mapper/VerificationStateMapper.java src/test/java/com/incode/verification/repository src/test/java/com/incode/verification/mapper
    git commit -m "refactor: simplify repository and coordination layers"

## Task 7: Consolidate configuration and exception handling

**Files:**

- Move: src/main/java/com/incode/verification/configuration/* to config/*
- Move: src/main/java/com/incode/verification/application/exception/VerificationException.java to exception/base/BusinessException.java
- Move: src/main/java/com/incode/verification/application/exception/VerificationConflictException.java to exception/domain/VerificationConflictException.java
- Move: src/main/java/com/incode/verification/application/exception/VerificationNotFoundException.java to exception/domain/VerificationNotFoundException.java
- Move: src/main/java/com/incode/verification/application/exception/ProviderSubmissionException.java to exception/IntegrationException.java or a focused subclass
- Move: src/main/java/com/incode/verification/application/exception/CoordinationUnavailableException.java to exception/IntegrationException.java or a focused subclass
- Move: src/main/java/com/incode/verification/adapter/in/web/ApiExceptionHandler.java to exception/handler/GlobalExceptionHandler.java
- Create: src/main/java/com/incode/verification/exception/payload/ErrorResponse.java if the current response type is not already reusable
- Test: src/test/java/com/incode/verification/config/*
- Test: src/test/java/com/incode/verification/exception/handler/GlobalExceptionHandlerTest.java

**Interfaces:**

- Existing error codes, response properties, HTTP statuses, and ProblemDetail/error payload shape remain identical.
- Configuration bean names freeProvider, premiumProvider, freeProviderClient, premiumProviderClient, verificationLifetime, and profile conditions remain unchanged.
- ProviderRuntimeHints remains imported by provider HTTP configuration after package relocation.

- [x] **Step 1: Move configuration tests and exception-handler tests.**

Update package declarations and run focused tests to establish the expected bean and error contracts.

- [x] **Step 2: Move configuration classes into config subpackages.**

Keep properties prefixes and environment variable defaults unchanged. Do not mix business logic into configuration.

- [x] **Step 3: Move and simplify exception hierarchy.**

Use one BusinessException base, focused domain exceptions, and one GlobalExceptionHandler. Preserve the current provider, coordination, conflict, and not-found error mappings.

- [x] **Step 4: Run configuration and exception tests.**

    ./gradlew test --tests com.incode.verification.config.* --tests com.incode.verification.exception.* --no-daemon --console=plain

- [x] **Step 5: Commit the config and exception slice.**

    git add src/main/java/com/incode/verification/config src/main/java/com/incode/verification/exception src/test/java/com/incode/verification/config src/test/java/com/incode/verification/exception
    git commit -m "refactor: consolidate configuration and exceptions"

## Task 8: Replace hexagonal architecture tests with layered rules

**Files:**

- Delete: src/test/java/com/incode/verification/architecture/AdapterPackageStructureTest.java
- Delete: src/test/java/com/incode/verification/architecture/DomainPackageStructureTest.java
- Delete: src/test/java/com/incode/verification/architecture/HexagonalDependencyTest.java
- Delete: src/test/java/com/incode/verification/architecture/ModulithArchitectureTest.java
- Create: src/test/java/com/incode/verification/architecture/LayeredDependencyTest.java
- Create: src/test/java/com/incode/verification/architecture/LayerPackageStructureTest.java

**Interfaces:**

- Controller classes may depend on service and controller DTO packages, but not repository, client, or config implementation packages.
- Service classes may depend on service/model, repository contracts, client contracts, mapper, and exception packages, but not controller, client/dto, repository/entity, Spring MVC, JDBC, Redis, or HTTP client implementation packages.
- Client and repository implementations may not depend on controllers or controller DTOs.
- Service/model classes must have no Spring, JDBC, Redis, HTTP client, or Jackson framework dependencies.
- No production classes remain under adapter, application, domain, or application/port.

- [x] **Step 1: Write failing layered ArchUnit rules.**

Assert the dependency rules above and assert the old directories do not exist.

- [x] **Step 2: Run architecture tests and verify RED while old packages remain.**

    ./gradlew test --tests com.incode.verification.architecture.LayeredDependencyTest --tests com.incode.verification.architecture.LayerPackageStructureTest --no-daemon --console=plain

- [x] **Step 3: Replace module verification with the layered rules.**

Keep the service model framework-free and enforce the package boundaries with ArchUnit.

- [x] **Step 4: Run all architecture tests and verify GREEN.**

    ./gradlew test --tests com.incode.verification.architecture.* --no-daemon --console=plain

- [x] **Step 5: Commit the architecture-rule slice.**

    git add src/test/java/com/incode/verification/architecture
    git commit -m "test: enforce layered backend boundaries"

## Task 9: Remove obsolete packages and run the complete verification gate

**Files:**

- Delete: obsolete src/main/java/com/incode/verification/adapter directories
- Delete: obsolete src/main/java/com/incode/verification/application/port directories
- Delete: obsolete src/main/java/com/incode/verification/application/service directories
- Delete: obsolete src/main/java/com/incode/verification/application/result directory
- Delete: obsolete src/main/java/com/incode/verification/domain directory
- Modify: src/test/java/com/incode/verification/configuration/GradleStructureTest.java
- Modify: repository README or service README only if package/run instructions mention old paths

**Interfaces:**

- No public endpoint or provider contract changes.
- No provider simulator source changes.
- No new dependencies unless compilation proves a required Spring API is absent.

- [x] **Step 1: Search for stale package references.**

    rg -n 'verification\\.(adapter|application|domain)|adapter/|application/port|domain/' src/main src/test

Expected: no production imports or architecture assertions reference obsolete packages.

- [x] **Step 2: Remove empty old directories and update stale tests/docs.**

Use git rm only for files confirmed obsolete after all imports compile.

- [x] **Step 3: Run Java formatting and static checks.**

    ./gradlew spotlessApply
    ./gradlew spotlessJavaCheck spotlessKotlinGradleCheck checkstyleMain checkstyleTest

Expected: all formatting and Checkstyle tasks pass.

- [x] **Step 4: Run the complete Java verification gate.**

    ./gradlew fastCheck --no-daemon --console=plain

Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Run AOT/native verification.**

    ./gradlew processAot --no-daemon --console=plain

Expected: Spring AOT processing succeeds and generated hints include both provider DTOs. If the workspace provides a native image task, run that task as a second command and record its result; the current Temurin JDK does not provide native-image, so nativeCompile requires a GraalVM JDK.

- [x] **Step 6: Run provider quality without modifying provider sources.**

    cd ../company-check-provider
    bun run quality

Expected: formatting, TypeScript, lint, and all provider tests pass.

- [x] **Step 7: Inspect the final diff and status.**

    git diff --check
    git status --short

Confirm that only migration files and intentionally preserved pre-existing changes are present.

- [x] **Step 8: Commit the completed migration.**

    git add src/main/java src/test/java
    git commit -m "refactor: simplify backend into layered architecture"
