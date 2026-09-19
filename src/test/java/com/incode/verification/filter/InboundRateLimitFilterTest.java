package com.incode.verification.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.repository.InboundRateLimiter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class InboundRateLimitFilterTest {
  @Test
  void allowsBackendServiceRequestWhenLimiterAllows() {
    var invoked = new AtomicBoolean();
    var filter = new InboundRateLimitFilter(new FakeInboundRateLimiter(allowed()));
    var exchange = exchange("GET", "/backend-service");

    filter
        .filter(
            exchange,
            ignored -> {
              invoked.set(true);
              return Mono.empty();
            })
        .block();

    assertTrue(invoked.get());
  }

  @Test
  void rejectsBackendServiceRequestWhenLimiterRejects() {
    var filter =
        new InboundRateLimitFilter(new FakeInboundRateLimiter(rejected(Duration.ofSeconds(2))));
    var exchange = exchange("GET", "/backend-service");

    filter.filter(exchange, ignored -> Mono.error(new AssertionError("chain invoked"))).block();

    assertFalse(
        exchange.getResponse().isCommitted() && exchange.getResponse().getStatusCode() == null);
    assertTrue(exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS);
    assertTrue("2".equals(exchange.getResponse().getHeaders().getFirst("Retry-After")));
  }

  @Test
  void returnsServiceUnavailableWhenLimiterIsUnavailable() {
    var filter =
        new InboundRateLimitFilter(new FakeInboundRateLimiter(unavailable(Duration.ofSeconds(1))));
    var exchange = exchange("GET", "/backend-service");

    filter.filter(exchange, ignored -> Mono.error(new AssertionError("chain invoked"))).block();

    assertTrue(exchange.getResponse().getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE);
    assertTrue("1".equals(exchange.getResponse().getHeaders().getFirst("Retry-After")));
  }

  @Test
  void skipsNonBackendServiceRequests() {
    var limiter = new FakeInboundRateLimiter(rejected(Duration.ofSeconds(2)));
    var filter = new InboundRateLimitFilter(limiter);
    var invoked = new AtomicBoolean();
    var exchange = exchange("GET", "/verifications/123");

    filter
        .filter(
            exchange,
            ignored -> {
              invoked.set(true);
              return Mono.empty();
            })
        .block();

    assertTrue(invoked.get());
    assertFalse(limiter.called);
  }

  private static MockServerWebExchange exchange(String method, String path) {
    return MockServerWebExchange.from(
        MockServerHttpRequest.method(HttpMethod.valueOf(method), path).build());
  }

  private static InboundRateLimiter.Decision allowed() {
    return new InboundRateLimiter.Decision(
        InboundRateLimiter.Decision.Status.ALLOWED, Duration.ZERO);
  }

  private static InboundRateLimiter.Decision rejected(Duration retryAfter) {
    return new InboundRateLimiter.Decision(InboundRateLimiter.Decision.Status.REJECTED, retryAfter);
  }

  private static InboundRateLimiter.Decision unavailable(Duration retryAfter) {
    return new InboundRateLimiter.Decision(
        InboundRateLimiter.Decision.Status.UNAVAILABLE, retryAfter);
  }

  private static final class FakeInboundRateLimiter implements InboundRateLimiter {
    private final Decision decision;
    private boolean called;

    private FakeInboundRateLimiter(Decision decision) {
      this.decision = decision;
    }

    @Override
    public Mono<Decision> tryAcquire() {
      called = true;
      return Mono.just(decision);
    }
  }
}
