package com.incode.verification.repository.coordination;

import com.incode.verification.repository.CoordinationRepository.Lease;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Mono;

final class RedisLease implements Lease {
  private static final Logger log = LoggerFactory.getLogger(RedisLease.class);
  private static final String RELEASE =
      "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
  private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
      new DefaultRedisScript<>(RELEASE, Long.class);

  private final ReactiveStringRedisTemplate redis;
  private final String key;
  private final String token;
  private final boolean acquired;
  private final boolean degraded;

  RedisLease(
      ReactiveStringRedisTemplate redis,
      String key,
      String token,
      boolean acquired,
      boolean degraded) {
    this.redis = redis;
    this.key = key;
    this.token = token;
    this.acquired = acquired;
    this.degraded = degraded;
  }

  @Override
  public boolean acquired() {
    return acquired;
  }

  @Override
  public boolean degraded() {
    return degraded;
  }

  @Override
  public Mono<Void> release() {
    if (degraded || !acquired) {
      return Mono.empty();
    }
    return redis
        .execute(RELEASE_SCRIPT, List.of(key), token)
        .next()
        .then()
        .doOnError(exception -> log.debug("Redis lease release unavailable", exception))
        .onErrorResume(exception -> Mono.empty());
  }
}
