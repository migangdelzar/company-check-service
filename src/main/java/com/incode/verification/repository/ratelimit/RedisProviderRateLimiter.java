package com.incode.verification.repository.ratelimit;

import com.incode.verification.config.provider.ProviderRateLimitProperties;
import com.incode.verification.service.model.ProviderType;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Mono;

public final class RedisProviderRateLimiter implements ProviderRateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RedisProviderRateLimiter.class);
  private static final DefaultRedisScript<Long> ALLOW_SCRIPT =
      new DefaultRedisScript<>(
          "local current = redis.call('incr', KEYS[1]) "
              + "if current == 1 then redis.call('pexpire', KEYS[1], ARGV[1]) end "
              + "if current <= tonumber(ARGV[2]) then return 1 else return 0 end",
          Long.class);

  private final ReactiveStringRedisTemplate redis;
  private final ProviderRateLimitProperties properties;

  public RedisProviderRateLimiter(
      ReactiveStringRedisTemplate redis, ProviderRateLimitProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public Mono<Boolean> tryAcquire(ProviderType provider) {
    var limit = properties.forProvider(provider);
    var key = properties.keyPrefix() + provider.name().toLowerCase(Locale.ROOT);
    return redis
        .execute(
            ALLOW_SCRIPT,
            List.of(key),
            String.valueOf(limit.refreshPeriod().toMillis()),
            String.valueOf(limit.limitForPeriod()))
        .next()
        .map(allowed -> Long.valueOf(1L).equals(allowed))
        .doOnError(
            exception -> {
              log.warn("Redis rate limiter unavailable; rejecting provider={}", provider);
              log.debug("Redis rate limiter failure", exception);
            })
        .onErrorReturn(false);
  }
}
