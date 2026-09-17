package com.incode.verification.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.application.port.out.InboundRateLimiter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InboundRateLimitFilterTest {
  @Test
  void allowsBackendServiceRequestWhenLimiterAllows() throws Exception {
    var limiter = new FakeInboundRateLimiter(allowed());
    var filter = new InboundRateLimitFilter(limiter);
    var request = request("POST", "/backend-service");
    var response = new MockHttpServletResponse();
    var chain = new TrackingFilterChain();

    filter.doFilter(request, response, chain);

    assertTrue(chain.invoked.get());
    assertEquals(200, response.getStatus());
  }

  @Test
  void rejectsBackendServiceRequestWhenLimiterRejects() throws Exception {
    var limiter = new FakeInboundRateLimiter(rejected(Duration.ofSeconds(2)));
    var filter = new InboundRateLimitFilter(limiter);
    var request = request("POST", "/backend-service");
    var response = new MockHttpServletResponse();
    var chain = new TrackingFilterChain();

    filter.doFilter(request, response, chain);

    assertFalse(chain.invoked.get());
    assertEquals(429, response.getStatus());
    assertEquals("2", response.getHeader("Retry-After"));
  }

  @Test
  void returnsServiceUnavailableWhenLimiterIsUnavailable() throws Exception {
    var limiter = new FakeInboundRateLimiter(unavailable(Duration.ofSeconds(1)));
    var filter = new InboundRateLimitFilter(limiter);
    var request = request("POST", "/backend-service");
    var response = new MockHttpServletResponse();
    var chain = new TrackingFilterChain();

    filter.doFilter(request, response, chain);

    assertFalse(chain.invoked.get());
    assertEquals(503, response.getStatus());
    assertEquals("1", response.getHeader("Retry-After"));
  }

  @Test
  void skipsNonBackendServiceRequests() throws Exception {
    var limiter = new FakeInboundRateLimiter(rejected(Duration.ofSeconds(2)));
    var filter = new InboundRateLimitFilter(limiter);
    var request = request("GET", "/verifications/123");
    var response = new MockHttpServletResponse();
    var chain = new TrackingFilterChain();

    filter.doFilter(request, response, chain);

    assertTrue(chain.invoked.get());
    assertEquals(200, response.getStatus());
    assertFalse(limiter.called);
  }

  private static MockHttpServletRequest request(String method, String path) {
    var request = new MockHttpServletRequest(method, path);
    request.setRequestURI(path);
    return request;
  }

  private static InboundRateLimiter.Decision allowed() {
    return new InboundRateLimiter.Decision(
        InboundRateLimiter.Decision.Status.ALLOWED, Duration.ZERO);
  }

  private static InboundRateLimiter.Decision rejected(Duration retryAfter) {
    return new InboundRateLimiter.Decision(
        InboundRateLimiter.Decision.Status.REJECTED, retryAfter);
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
    public Decision tryAcquire() {
      called = true;
      return decision;
    }
  }

  private static final class TrackingFilterChain extends MockFilterChain {
    private final AtomicBoolean invoked = new AtomicBoolean();

    @Override
    public void doFilter(
        jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
      invoked.set(true);
    }
  }
}
