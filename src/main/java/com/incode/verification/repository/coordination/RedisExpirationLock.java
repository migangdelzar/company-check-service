package com.incode.verification.repository.coordination;

import com.incode.verification.config.coordination.ExpirationLockProperties;
import com.incode.verification.repository.ExpirationLock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisExpirationLock implements ExpirationLock {
  private static final Logger log = LoggerFactory.getLogger(RedisExpirationLock.class);
  private static final String RELEASE =
      "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
  private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
      new DefaultRedisScript<>(RELEASE, Long.class);

  private final StringRedisTemplate redis;
  private final ExpirationLockProperties properties;

  public RedisExpirationLock(StringRedisTemplate redis, ExpirationLockProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public Lease tryAcquire() {
    String token = UUID.randomUUID().toString();
    try {
      Boolean acquired = redis.opsForValue().setIfAbsent(properties.key(), token, properties.ttl());
      return new RedisLease(redis, properties.key(), token, Boolean.TRUE.equals(acquired));
    } catch (Exception exception) {
      log.debug("Redis expiration lock unavailable; skipping expiration", exception);
      return new RedisLease(redis, properties.key(), token, false);
    }
  }

  private static final class RedisLease implements Lease {
    private final StringRedisTemplate redis;
    private final String key;
    private final String token;
    private final boolean acquired;
    private final AtomicBoolean closed = new AtomicBoolean();

    private RedisLease(StringRedisTemplate redis, String key, String token, boolean acquired) {
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
    public void close() {
      if (!acquired || !closed.compareAndSet(false, true)) {
        return;
      }
      try {
        redis.execute(RELEASE_SCRIPT, List.of(key), token);
      } catch (Exception exception) {
        log.debug("Redis expiration lock release unavailable", exception);
      }
    }
  }
}
