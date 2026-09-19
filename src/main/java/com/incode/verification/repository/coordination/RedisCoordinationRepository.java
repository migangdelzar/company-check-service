package com.incode.verification.repository.coordination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.config.coordination.CoordinationProperties;
import com.incode.verification.repository.CoordinationRepository;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.VerificationResult;
import com.incode.verification.util.UuidV7;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RedisCoordinationRepository implements CoordinationRepository {
  private static final Logger log = LoggerFactory.getLogger(RedisCoordinationRepository.class);
  private static final String TAKEOVER =
      "if redis.call('exists',KEYS[1])==0 then return redis.call('set',KEYS[1],ARGV[1],'NX','PX',"
          + "ARGV[2]) else return nil end";
  private static final DefaultRedisScript<String> TAKEOVER_SCRIPT =
      new DefaultRedisScript<>(TAKEOVER, String.class);
  private final ReactiveStringRedisTemplate redis;
  private final CacheManager cacheManager;
  private final CoordinationProperties properties;
  private final ObjectMapper mapper;

  public RedisCoordinationRepository(
      ReactiveStringRedisTemplate redis,
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
  public Mono<VerificationResult> get(NormalizedQuery query) {
    String cacheKey = keyForCache(query);
    var cache = cacheManager.getCache("verification");
    return Mono.justOrEmpty(
            cache == null ? null : cache.get(query.value(), VerificationResult.class))
        .switchIfEmpty(
            redis
                .opsForValue()
                .get(cacheKey)
                .flatMap(this::decode)
                .doOnNext(result -> putLocal(cache, query, result)))
        .doOnError(
            exception ->
                log.debug("Redis cache read unavailable; treating cache as a miss", exception))
        .onErrorResume(exception -> Mono.empty());
  }

  @Override
  @Observed(name = "verification.cache.write")
  public Mono<VerificationResult> put(NormalizedQuery query, VerificationResult result) {
    String cacheKey = keyForCache(query);
    var cache = cacheManager.getCache("verification");
    return Mono.defer(
            () -> {
              try {
                long millis = ttlMillis(result);
                String json = mapper.writeValueAsString(result);
                return redis
                    .opsForValue()
                    .set(cacheKey, json, Duration.ofMillis(millis))
                    .thenReturn(result);
              } catch (Exception exception) {
                return Mono.error(exception);
              }
            })
        .doOnNext(value -> putLocal(cache, query, value))
        .doOnError(
            exception ->
                log.debug("Redis cache write unavailable; continuing with local cache", exception))
        .onErrorResume(exception -> Mono.just(result));
  }

  @Override
  @Observed(name = "verification.coordination.acquire")
  public Mono<Lease> acquire(NormalizedQuery query) {
    String leaseKey = properties.keyPrefix() + "lease:" + query.value();
    String token = UuidV7.generate().toString();
    return redis
        .opsForValue()
        .setIfAbsent(leaseKey, token, properties.leaseTtl())
        .flatMap(
            acquired -> {
              if (Boolean.TRUE.equals(acquired)) {
                return Mono.just(new RedisLease(redis, leaseKey, token, true, false));
              }
              return waitForCachedResult(query)
                  .map(ignored -> (Lease) new RedisLease(redis, leaseKey, token, false, false))
                  .switchIfEmpty(takeOver(leaseKey, token));
            })
        .doOnError(
            exception ->
                log.debug(
                    "Redis coordination unavailable; refusing external lookup ownership",
                    exception))
        .onErrorResume(exception -> Mono.just(new RedisLease(redis, leaseKey, token, false, true)));
  }

  private Mono<Boolean> waitForCachedResult(NormalizedQuery query) {
    return Flux.range(0, properties.waiterAttempts())
        .concatMap(
            ignored ->
                get(query)
                    .hasElement()
                    .flatMap(
                        cached ->
                            cached
                                ? Mono.just(true)
                                : Mono.delay(properties.waiterPoll()).thenReturn(false)))
        .filter(Boolean::booleanValue)
        .next();
  }

  private Mono<Lease> takeOver(String leaseKey, String token) {
    return redis
        .execute(
            TAKEOVER_SCRIPT,
            java.util.List.of(leaseKey),
            token,
            String.valueOf(properties.leaseTtl().toMillis()))
        .next()
        .map(value -> (Lease) new RedisLease(redis, leaseKey, token, "OK".equals(value), false))
        .defaultIfEmpty(new RedisLease(redis, leaseKey, token, false, false));
  }

  private Mono<VerificationResult> decode(String json) {
    try {
      return Mono.just(mapper.readValue(json, VerificationResult.class));
    } catch (Exception exception) {
      return Mono.error(exception);
    }
  }

  private void putLocal(@Nullable Cache cache, NormalizedQuery query, VerificationResult result) {
    if (cache != null) {
      cache.put(query.value(), result);
    }
  }

  private long ttlMillis(VerificationResult result) {
    return properties.ttlFor(result).toMillis()
        + ThreadLocalRandom.current().nextLong(properties.jitter().toMillis() + 1);
  }

  private String keyForCache(NormalizedQuery query) {
    return properties.keyPrefix() + "cache:v1:" + query.value();
  }
}
