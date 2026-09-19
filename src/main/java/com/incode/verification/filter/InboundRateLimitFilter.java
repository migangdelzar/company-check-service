package com.incode.verification.filter;

import com.incode.verification.repository.InboundRateLimiter;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public final class InboundRateLimitFilter implements WebFilter {
  private static final String BACKEND_SERVICE_PATH = "/backend-service";
  private static final String RETRY_AFTER = "Retry-After";

  private final InboundRateLimiter limiter;

  public InboundRateLimitFilter(InboundRateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    var request = exchange.getRequest();
    if (!Objects.equals(request.getMethod(), HttpMethod.GET)
        || !BACKEND_SERVICE_PATH.equals(request.getPath().value())) {
      return chain.filter(exchange);
    }
    return limiter
        .tryAcquire()
        .flatMap(
            decision -> {
              if (decision.allowed()) {
                return chain.filter(exchange);
              }
              var response = exchange.getResponse();
              response.setStatusCode(
                  decision.status() == InboundRateLimiter.Decision.Status.REJECTED
                      ? org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
                      : org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
              response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
              var retryAfter = retryAfterSeconds(decision.retryAfter());
              if (retryAfter > 0) {
                response.getHeaders().set(RETRY_AFTER, Long.toString(retryAfter));
              }
              return response.setComplete();
            });
  }

  private static long retryAfterSeconds(Duration duration) {
    if (duration.isZero() || duration.isNegative()) {
      return 0;
    }
    return Math.max(1, (duration.toMillis() + 999) / 1000);
  }
}
