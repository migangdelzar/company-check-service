package com.incode.verification.repository.coordination;

import com.incode.verification.config.coordination.ExpirationLockProperties;
import com.incode.verification.repository.ExpirationLock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Mono;

public final class RedisExpirationLock implements ExpirationLock {
  private static final Logger log = LoggerFactory.getLogger(RedisExpirationLock.class);
  private static final String RELEASE =
      "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
  private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
      new DefaultRedisScript<>(RELEASE, Long.class);

  private final ReactiveStringRedisTemplate redis;
  private final ExpirationLockProperties properties;

  public RedisExpirationLock(
      ReactiveStringRedisTemplate redis, ExpirationLockProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public Mono<Lease> tryAcquire() {
    String token = UUID.randomUUID().toString();
    return Mono.defer(
            () -> redis.opsForValue().setIfAbsent(properties.key(), token, properties.ttl()))
        .map(acquired -> (Lease) new RedisLease(redis, properties.key(), token, acquired))
        .doOnError(
            exception ->
                log.debug("Redis expiration lock unavailable; skipping expiration", exception))
        .onErrorReturn(new RedisLease(redis, properties.key(), token, false));
  }

  private static final class RedisLease implements Lease {
    private final ReactiveStringRedisTemplate redis;
    private final String key;
    private final String token;
    private final boolean acquired;
    private final AtomicBoolean closed = new AtomicBoolean();

    private RedisLease(
        ReactiveStringRedisTemplate redis, String key, String token, boolean acquired) {
      this.redis = redis;
      this.key = key;
      this.token = token;
      this.acquired = acquired;
    }

    @Override
    public boolean acquired() {
      return acquired;
    }

    @Override
    public Mono<Void> release() {
      if (!acquired || !closed.compareAndSet(false, true)) {
        return Mono.empty();
      }
      return redis
          .execute(RELEASE_SCRIPT, List.of(key), token)
          .next()
          .then()
          .doOnError(exception -> log.debug("Redis expiration lock release unavailable", exception))
          .onErrorResume(exception -> Mono.empty());
    }
  }
}
