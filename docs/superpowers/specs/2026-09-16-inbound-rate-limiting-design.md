# Inbound and Provider Rate-Limiting Design

## Goal

Protect the public `POST /backend-service` endpoint from overload while
preserving provider-specific quotas and concurrency isolation. The service
will use a web admission filter for inbound traffic and keep the existing
provider resilience stack for external provider calls.

## Current context

- Provider HTTP calls already use one Apache HttpClient connection pool with
  `maxConnTotal=100` and `maxConnPerRoute=100`.
- Provider methods already use Resilience4j retry, circuit breaker, rate
  limiter, and bulkhead annotations in the single-node profile.
- The distributed profile already uses a Redis-backed provider rate limiter.
- Hikari already limits database connections to 32.
- Redis operations use Spring Data Redis/Lettuce; no dedicated blocking Redis
  pool is currently required by the short cache, lease, and Lua-script calls.
- UUID reuse and idempotency remain application-service behavior. The web
  filter must not query verification state.

## Decision

Add a Spring `OncePerRequestFilter` that matches only `POST /backend-service`.
The filter delegates to an injected `InboundRateLimiter` port and returns HTTP
`429 Too Many Requests` before controller binding or application-service work
when the limit is exceeded.

The first version uses one service-wide inbound bucket because the current API
has no authenticated client or tenant identity. It will not trust arbitrary
forwarded IP headers. A per-client limiter can be added later when a trusted
client identity contract exists.

### Profile-specific implementations

Single-node wiring uses one Resilience4j limiter instance named
`backendService`. The filter calls a Spring-managed admission service so the
Resilience4j proxy is applied; the servlet filter itself is not annotated with
`@RateLimiter`.

Distributed wiring uses a Redis atomic fixed-window script under a dedicated
inbound key prefix. The implementation returns a rejection window so the
filter can emit `Retry-After`. It must fail closed if Redis is unavailable,
because accepting traffic during a distributed limiter outage would bypass
the protection guarantee.

The inbound limiter is separate from `ProviderRateLimiter`: inbound traffic
and provider calls have different limits, keys, failure policies, and owners.

## Request flow

```text
HTTP POST /backend-service
  -> inbound OncePerRequestFilter
       -> allow: continue to MVC/controller
       -> reject: 429 + Retry-After
  -> request validation
  -> StartVerificationService UUID/idempotency check
  -> provider resolution
       -> provider rate limiter
       -> bulkhead
       -> retry/circuit breaker
       -> pooled HTTP client
  -> response
```

The existing UUID behavior is unchanged:

- same UUID and same query with a completed result returns the stored result;
- same UUID with a different query returns `409 VERIFICATION_ID_REUSE`;
- same UUID while in progress returns `409 VERIFICATION_IN_PROGRESS`;
- the database conflict path remains the race-safe authority.

## Configuration

Add a dedicated inbound configuration namespace, separate from provider
limits. The single-node profile configures the Resilience4j `backendService`
instance. The distributed profile configures the Redis window limit and key
prefix. The provider rate-limit configuration remains unchanged.

The existing provider HTTP pool remains shared initially. Its current size is
coherent with the two provider bulkheads: at most 50 concurrent calls per
provider and at most 100 pooled connections total. Pool sizing is concurrency
control, not request-rate control, so it does not replace either inbound or
provider rate limiting.

## Error handling

| Situation | Behavior |
|---|---|
| Inbound permit available | Continue the filter chain. |
| Inbound limit exceeded | Return `429` and `Retry-After`; do not call the controller. |
| Resilience4j limiter rejects | Convert the rejection to the same `429` response. |
| Redis limiter rejects | Return `429` with the Redis-provided retry window. |
| Redis limiter unavailable in distributed mode | Fail closed with `503 Service Unavailable`; the implementation must not silently allow traffic. |
| Invalid request | Existing MVC validation behavior remains unchanged. |
| Existing verification UUID | Existing application conflict/idempotency behavior remains unchanged. |

Redis outage uses `503 Service Unavailable`, because the request was not
rejected for quota exhaustion; it was rejected because the protection
dependency is unavailable.

## Testing strategy

- Unit-test the filter for allowed requests, rejected requests, path/method
  matching, response status, and `Retry-After`.
- Unit-test the Resilience4j admission adapter with a low test limit.
- Unit-test the Redis limiter for atomic allow/reject behavior and outage
  fail-closed behavior using the existing Redis test conventions.
- Add profile configuration tests proving that single-node creates the local
  inbound limiter and distributed creates the Redis inbound limiter without
  creating both.
- Add a web integration/slice test proving rejected `/backend-service`
  requests never invoke the use case.
- Preserve provider limiter, bulkhead, HTTP-pool, UUID conflict, and existing
  API contract tests.

## Non-goals

- No replacement of provider rate limiting.
- No replacement of provider bulkheads or the Apache HTTP connection pool.
- No UUID lookup in a servlet filter.
- No trust of unvalidated `X-Forwarded-For` data.
- No per-client quota until a trusted client identity is available.
- No Redis connection-pool tuning without load-test evidence.
