package com.incode.verification.repository.coordination;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.config.coordination.ExpirationLockProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class RedisExpirationLockTest {
  @Test
  void acquiresAndReleasesThroughReactiveRedis() {
    var redis = mock(ReactiveStringRedisTemplate.class);
    var values = mock(ReactiveValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(Mono.just(true));
    when(redis.execute(any(), any(), anyString())).thenReturn(Flux.just(1L));
    var lock =
        new RedisExpirationLock(
            redis, new ExpirationLockProperties("test:expiration", Duration.ofSeconds(30)));

    var lease = lock.tryAcquire().block();
    assertTrue(lease.acquired());
    lease.release().block();
    verify(redis).execute(any(), any(), anyString());
  }

  @Test
  void failsClosedWhenRedisCannotAcquireTheLock() {
    var redis = mock(ReactiveStringRedisTemplate.class);
    when(redis.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
    var lock =
        new RedisExpirationLock(
            redis, new ExpirationLockProperties("test:expiration", Duration.ofSeconds(30)));

    assertFalse(lock.tryAcquire().block().acquired());
  }
}
