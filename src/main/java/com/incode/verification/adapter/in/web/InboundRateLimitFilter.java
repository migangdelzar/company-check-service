package com.incode.verification.adapter.in.web;

import com.incode.verification.application.port.out.InboundRateLimiter;
import java.io.IOException;
import java.time.Duration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class InboundRateLimitFilter extends OncePerRequestFilter {
  private static final String BACKEND_SERVICE_PATH = "/backend-service";
  private static final String RETRY_AFTER = "Retry-After";

  private final InboundRateLimiter limiter;

  public InboundRateLimitFilter(InboundRateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !("GET".equals(request.getMethod())
        && BACKEND_SERVICE_PATH.equals(request.getRequestURI()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var decision = limiter.tryAcquire();
    if (decision.allowed()) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(decision.status() == InboundRateLimiter.Decision.Status.REJECTED ? 429 : 503);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    var retryAfter = retryAfterSeconds(decision.retryAfter());
    if (retryAfter > 0) {
      response.setHeader(RETRY_AFTER, Long.toString(retryAfter));
    }
  }

  private static long retryAfterSeconds(Duration duration) {
    if (duration.isZero() || duration.isNegative()) {
      return 0;
    }
    return Math.max(1, (duration.toMillis() + 999) / 1000);
  }
}
