package com.incode.verification.configuration;

import com.github.benmanes.caffeine.cache.Expiry;
import com.incode.verification.application.result.VerificationResult;

final class VerificationCacheExpiry implements Expiry<Object, Object> {
  private final CoordinationProperties properties;

  VerificationCacheExpiry(CoordinationProperties properties) {
    this.properties = properties;
  }

  @Override
  public long expireAfterCreate(Object key, Object value, long now) {
    return properties.ttlFor((VerificationResult) value).toNanos();
  }

  @Override
  public long expireAfterUpdate(Object key, Object value, long now, long currentDuration) {
    return currentDuration;
  }

  @Override
  public long expireAfterRead(Object key, Object value, long now, long currentDuration) {
    return currentDuration;
  }
}
