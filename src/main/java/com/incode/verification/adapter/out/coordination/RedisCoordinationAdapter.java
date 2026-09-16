package com.incode.verification.adapter.out.coordination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.incode.verification.adapter.config.CoordinationProperties;
import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.domain.valueobject.LookupKey;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisCoordinationAdapter implements CoordinationPort {
  private static final String RELEASE =
      "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
  private static final String TAKEOVER =
      "if redis.call('exists',KEYS[1])==0 then return redis.call('set',KEYS[1],ARGV[1],'NX','PX',ARGV[2]) else return nil end";
  private final Cache<String, VerificationView> l1;
  private final StringRedisTemplate redis;
  private final CoordinationProperties properties;
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  public RedisCoordinationAdapter(
      Cache<String, VerificationView> l1,
      StringRedisTemplate redis,
      CoordinationProperties properties) {
    this.l1 = l1;
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public Optional<VerificationView> cached(LookupKey key) {
    String k = cacheKey(key);
    VerificationView value = l1.getIfPresent(k);
    if (value != null) return Optional.of(value);
    try {
      String json = redis.opsForValue().get(k);
      if (json == null) return Optional.empty();
      VerificationView view = mapper.readValue(json, VerificationView.class);
      l1.put(k, view);
      return Optional.of(view);
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  @Override
  public void cache(LookupKey key, VerificationView view) {
    String k = cacheKey(key);
    l1.put(k, view);
    try {
      long millis = ttlMillis();
      redis.opsForValue().set(k, mapper.writeValueAsString(view), Duration.ofMillis(millis));
    } catch (Exception ignored) {
    }
  }

  @Override
  public Lease acquire(LookupKey key) {
    String leaseKey = properties.keyPrefix() + "lease:" + key.query().value();
    String token = UUID.randomUUID().toString();
    try {
      Boolean acquired = redis.opsForValue().setIfAbsent(leaseKey, token, properties.leaseTtl());
      if (Boolean.TRUE.equals(acquired)) return new RedisLease(leaseKey, token, true, false);
      for (int i = 0; i < properties.waiterAttempts(); i++) {
        if (cached(key).isPresent()) return new RedisLease(leaseKey, token, false, false);
        Thread.sleep(properties.waiterPoll().toMillis());
      }
      String takeover =
          redis.execute(
              new DefaultRedisScript<>(TAKEOVER, String.class),
              java.util.List.of(leaseKey),
              token,
              String.valueOf(properties.leaseTtl().toMillis()));
      return new RedisLease(leaseKey, token, "OK".equals(takeover), false);
    } catch (Exception ignored) {
      return new RedisLease(leaseKey, token, true, true);
    }
  }

  private long ttlMillis() {
    return properties.ttl().toMillis()
        + ThreadLocalRandom.current().nextLong(properties.jitter().toMillis() + 1);
  }

  private String cacheKey(LookupKey key) {
    return properties.keyPrefix() + "cache:v1:" + key.query().value();
  }

  private final class RedisLease implements Lease {
    private final String key, token;
    private final boolean acquired, failOpen;

    RedisLease(String key, String token, boolean acquired, boolean failOpen) {
      this.key = key;
      this.token = token;
      this.acquired = acquired;
      this.failOpen = failOpen;
    }

    public boolean acquired() {
      return acquired;
    }

    public boolean failOpen() {
      return failOpen;
    }

    public void close() {
      if (!failOpen)
        try {
          redis.execute(
              new DefaultRedisScript<>(RELEASE, Long.class), java.util.List.of(key), token);
        } catch (Exception ignored) {
        }
    }
  }
}
