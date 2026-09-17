package com.incode.verification.adapter.out.coordination;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.application.port.out.ExpirationLock;
import com.incode.verification.configuration.ExpirationLockProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedisExpirationLockTest {
  @Test
  void acquiresWithTokenAndReleasesOnlyThroughTheRedisScript() {
    var redis = mock(StringRedisTemplate.class);
    var values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(eq("test:expiration"), anyString(), eq(Duration.ofSeconds(30))))
        .thenReturn(true);
    var lock =
        new RedisExpirationLock(
            redis, new ExpirationLockProperties("test:expiration", Duration.ofSeconds(30)));

    var lease = lock.tryAcquire();

    assertTrue(lease.acquired());
    verify(values).setIfAbsent(eq("test:expiration"), anyString(), eq(Duration.ofSeconds(30)));
    lease.close();
    verify(redis)
        .execute(any(DefaultRedisScript.class), eq(List.of("test:expiration")), anyString());
  }

  @Test
  void failsClosedWhenRedisCannotAcquireTheLock() {
    var redis = mock(StringRedisTemplate.class);
    when(redis.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
    var lock =
        new RedisExpirationLock(
            redis, new ExpirationLockProperties("test:expiration", Duration.ofSeconds(30)));

    ExpirationLock.Lease lease = lock.tryAcquire();

    assertFalse(lease.acquired());
  }
}
