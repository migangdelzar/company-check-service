package com.incode.verification.repository.coordination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.config.coordination.CoordinationProperties;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.VerificationResult;
import com.incode.verification.util.UuidV7;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisCoordinationRepository implements CoordinationRepository {
  private static final Logger log = LoggerFactory.getLogger(RedisCoordinationRepository.class);
  private static final String TAKEOVER =
      "if redis.call('exists',KEYS[1])==0 then return redis.call('set',KEYS[1],ARGV[1],'NX','PX',"
          + "ARGV[2]) else return nil end";
  private static final DefaultRedisScript<String> TAKEOVER_SCRIPT =
      new DefaultRedisScript<>(TAKEOVER, String.class);
  private final StringRedisTemplate redis;
  private final CacheManager cacheManager;
  private final CoordinationProperties properties;
  private final ObjectMapper mapper;

  public RedisCoordinationRepository(
      StringRedisTemplate redis,
      CacheManager cacheManager,
      CoordinationProperties properties,
      ObjectMapper mapper) {
    this.redis = redis;
    this.cacheManager = cacheManager;
    this.properties = properties;
    this.mapper = mapper;
  }

  @Override
  @Observed(name = "verification.cache.read")
  @Cacheable(cacheNames = "verification", key = "#query.value()", unless = "#result.isEmpty()")
  public Optional<VerificationResult> get(NormalizedQuery query) {
    String cacheKey = keyForCache(query);
    try {
      String json = redis.opsForValue().get(cacheKey);
      if (json == null) {
        return Optional.empty();
      }
      return Optional.of(mapper.readValue(json, VerificationResult.class));
    } catch (Exception exception) {
      log.debug("Redis cache read unavailable; treating cache as a miss", exception);
      return Optional.empty();
    }
  }

  @Override
  @Observed(name = "verification.cache.write")
  @CachePut(cacheNames = "verification", key = "#query.value()")
  public VerificationResult put(NormalizedQuery query, VerificationResult result) {
    String cacheKey = keyForCache(query);
    try {
      long millis = ttlMillis(result);
      redis
          .opsForValue()
          .set(cacheKey, mapper.writeValueAsString(result), Duration.ofMillis(millis));
    } catch (Exception exception) {
      log.debug("Redis cache write unavailable; continuing with local cache", exception);
    }
    return result;
  }

  @Override
  @Observed(name = "verification.coordination.acquire")
  public Lease acquire(NormalizedQuery query) {
    String leaseKey = properties.keyPrefix() + "lease:" + query.value();
    String token = UuidV7.generate().toString();
    try {
      Boolean acquired = redis.opsForValue().setIfAbsent(leaseKey, token, properties.leaseTtl());
      if (Boolean.TRUE.equals(acquired)) {
        return new RedisLease(redis, leaseKey, token, true, false);
      }
      for (int i = 0; i < properties.waiterAttempts(); i++) {
        if (cached(query)) {
          return new RedisLease(redis, leaseKey, token, false, false);
        }
        Thread.sleep(properties.waiterPoll().toMillis());
      }
      String takeover =
          redis.execute(
              TAKEOVER_SCRIPT,
              java.util.List.of(leaseKey),
              token,
              String.valueOf(properties.leaseTtl().toMillis()));
      return new RedisLease(redis, leaseKey, token, "OK".equals(takeover), false);
    } catch (Exception exception) {
      log.debug("Redis coordination unavailable; refusing external lookup ownership", exception);
      return new RedisLease(redis, leaseKey, token, false, true);
    }
  }

  private boolean cached(NormalizedQuery query) {
    var cache = cacheManager.getCache("verification");
    if (cache == null) {
      return false;
    }
    return cache.get(query.value(), VerificationResult.class) != null;
  }

  private long ttlMillis(VerificationResult result) {
    return properties.ttlFor(result).toMillis()
        + ThreadLocalRandom.current().nextLong(properties.jitter().toMillis() + 1);
  }

  private String keyForCache(NormalizedQuery query) {
    return properties.keyPrefix() + "cache:v1:" + query.value();
  }
}
