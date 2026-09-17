# Simplify Backend to Conventional Layered Architecture

## Scope

Migrate only company-check-service from the current DDD/hexagonal package
structure to a conventional layered Spring Boot structure. The
company-check-provider simulator is out of scope and keeps its current
structure.

The migration is structural and readability-focused. It must preserve the
existing HTTP API, provider contracts, persistence behavior, resilience
behavior, coordination behavior, scheduled expiration, observability, and
native-image support.

## Target structure

The approved conventional top-level layout is:

    config/
    controller/
      api/              optional, only for explicit API interfaces
      dto/request/
      dto/response/
    service/
      contract/         only for useful service interfaces
      impl/             only when multiple implementations are meaningful
      validator/
      event/            only when application events exist
      model/
    repository/
      entity/
      projection/       only when read projections exist
    client/
      dto/
    exception/
      handler/
      base/
      domain/
      payload/
    mapper/
    aspect/
    security/
    util/
    constant/

Only packages required by current behavior will be created during this
migration.

util/ is available only for genuinely shared, stateless helpers. No utility
class will be created merely to satisfy the directory tree.

The service/model/ package contains small internal records needed between the
service, repository, and mapper layers. These are not domain aggregates and do
not introduce a new modeling framework.

## Layer responsibilities

## Current-to-target mapping

The migration maps existing code as follows:

| Current location or responsibility | New location |
| --- | --- |
| configuration/* | config/* |
| adapter/in/web/* | controller/* |
| web request and response records | controller/dto/request and controller/dto/response |
| adapter/in/web/ApiExceptionHandler | exception/handler/GlobalExceptionHandler |
| application/service/* | service/* |
| application/port/in/* | service/contract/* only where a public service contract is useful |
| application/port/out/VerificationRepository | repository/VerificationRepository |
| adapter/out/persistence/* | repository/* and repository/entity/* |
| adapter/out/provider/* | client/* |
| provider wire DTOs | client/dto/* |
| application/port/out/ProviderLookupPort | client/ProviderClient |
| adapter/out/coordination/* | repository/coordination/* |
| adapter/out/ratelimit/* | client or aspect, according to whether it limits provider calls or inbound HTTP |
| domain business records and enums | service/model/* |
| ProviderResponseMapper and VerificationStateCodec | mapper/* |
| application/exception/* | exception/base, exception/domain, and exception/IntegrationException |
| Resilience4j and observation annotations | client or aspect; no new custom aspect unless behavior is actually cross-cutting |

The expanded layout is a menu, not a requirement to create unused packages.
There is currently no application security subsystem, OpenAPI contract layer,
async executor, domain event flow, database projection, audit aspect, or
application-wide constant set. Those directories stay absent until the
corresponding behavior is introduced.

### controller

Owns HTTP concerns only:

- Spring MVC annotations and route definitions.
- Request validation and API request DTOs.
- Response DTOs and API serialization shape.
- Translation of service exceptions through one global exception handler.

Controllers do not call repositories or provider clients directly.

### service

Owns the business workflow:

- Start and retrieve verification operations.
- Verification ID reuse and in-progress conflict rules.
- FREE-first/PREMIUM-fallback provider selection.
- Active-company filtering and otherResults behavior.
- Cache/shared-result recovery and persistence orchestration.
- Expiration use cases.

Services depend on simple repository/client abstractions where substitution is
needed for tests. These abstractions are named for their responsibility, not
as in/out ports.

Concrete service classes are preferred. impl/ is only used if an interface has
multiple meaningful implementations; no interface-plus-implementation pair
will be added mechanically.

### repository

Owns external state and I/O:

- JDBC access and persistence entities.
- Redis/local coordination.
- Rate-limiter implementations.

Persistence entities never leave this layer. Mappers convert them into service
models.

### client

Owns external HTTP/RPC integrations:

- FREE and PREMIUM provider clients.
- Provider-specific request/response DTOs.
- Provider resilience wrappers and response mapping.

Provider DTOs never leave this layer. Mappers convert them into service models.

### config

Owns Spring wiring, properties, profiles, HTTP clients, resilience setup,
Jackson configuration, rate limits, database configuration, and runtime hints.
Configuration classes may connect layers but do not contain business decisions.

### exception

Owns application errors and API error translation. GlobalExceptionHandler
replaces adapter-specific naming while preserving existing error codes and HTTP
status mappings.

### mapper

Owns explicit conversions between:

- controller request/response DTOs;
- service models;
- repository entities;
- provider DTOs.

Mappers remain small and deterministic. No reflection-based general-purpose
mapper will be introduced.

## Main request flow

    HTTP request
      → controller request DTO
      → VerificationService
          → Repository lookup / coordination
          → ProviderService
              → FreeProviderClient
              → PremiumProviderClient when required
          → mapper
          → repository/entity persistence
      → controller response DTO

The provider flow remains:

1. Call FREE with query.
2. Use PREMIUM when FREE returns HTTP 503, another configured transient
   failure, a malformed response, or an empty result.
3. Filter inactive companies before creating the backend response.
4. Return the first active company and any remaining active matches as
   otherResults.

The provider simulator and its exact FREE snake_case/PREMIUM camelCase
contracts are unchanged by this migration.

## DTO and native-image design

Provider response DTOs remain provider-specific:

- FreeProviderCompanyDto maps cin, name, registration_date, address, and
  is_active.
- PremiumProviderCompanyDto maps companyIdentificationNumber, companyName,
  registrationDate, companyFullAddress, and isActive.

They live under client/dto after migration. A focused runtime hints registrar in
config registers both DTOs for Jackson binding in
GraalVM native images. The request parameter is represented as the existing
query URI variable because both third-party contracts use the same parameter;
separate request classes would not add information.

## Migration sequence

1. Finish and verify typed provider DTOs and native-image hints.
2. Introduce target controller DTOs, service models, repository entities, and
   mappers without changing endpoint behavior.
3. Move and simplify provider clients and provider resilience handling.
4. Move JDBC, coordination, cache, and rate-limit implementations.
5. Consolidate Spring configuration under config.
6. Move service workflows and replace ports/adapters terminology.
7. Move exceptions and global HTTP error handling.
8. Replace hexagonal package tests with layered dependency tests.
9. Remove obsolete packages and compatibility shims.
10. Run formatting, static analysis, unit tests, integration tests, provider
    contract tests, AOT/native verification, and the workspace fast gate.

Each step must leave the service compilable and testable. Existing unrelated
working-tree changes must be preserved.

## Verification criteria

- No production classes remain under domain, adapter, or application/port.
- Controllers depend on services, never repositories or provider clients.
- Services do not depend on Spring MVC, JDBC, Redis, or provider wire DTOs.
- Repository/provider implementations do not leak entities or wire DTOs into
  controller responses.
- Existing endpoint paths, request fields, response fields, error codes, and
  provider fallback behavior remain unchanged.
- Architecture tests enforce the new dependency direction.
- ./gradlew fastCheck passes.
- Provider bun run quality passes.
- Spring AOT processing succeeds with provider DTO runtime hints registered.
