package com.incode.verification.repository.ratelimit;

import com.incode.verification.config.ratelimit.InboundRateLimitProperties;
import com.incode.verification.repository.InboundRateLimiter;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Mono;

public final class RedisInboundRateLimiter implements InboundRateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RedisInboundRateLimiter.class);
  private static final String KEY_SUFFIX = "backend-service";
  private static final DefaultRedisScript<Long> ALLOW_SCRIPT =
      new DefaultRedisScript<>(
          "local current = redis.call('incr', KEYS[1]) "
              + "if current == 1 then redis.call('pexpire', KEYS[1], ARGV[1]) end "
              + "if current <= tonumber(ARGV[2]) then return 1 else return 0 end",
          Long.class);

  private final ReactiveStringRedisTemplate redis;
  private final InboundRateLimitProperties properties;

  public RedisInboundRateLimiter(
      ReactiveStringRedisTemplate redis, InboundRateLimitProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public Mono<Decision> tryAcquire() {
    var retryAfter = properties.refreshPeriod();
    var key = properties.keyPrefix() + KEY_SUFFIX;
    return redis
        .execute(
            ALLOW_SCRIPT,
            List.of(key),
            String.valueOf(retryAfter.toMillis()),
            String.valueOf(properties.limitForPeriod()))
        .next()
        .map(
            allowed ->
                Long.valueOf(1L).equals(allowed)
                    ? new Decision(Decision.Status.ALLOWED, Duration.ZERO)
                    : new Decision(Decision.Status.REJECTED, retryAfter))
        .doOnError(
            exception -> {
              log.warn("Redis inbound rate limiter unavailable");
              log.debug("Redis inbound rate limiter failure", exception);
            })
        .onErrorReturn(new Decision(Decision.Status.UNAVAILABLE, retryAfter));
  }
}
