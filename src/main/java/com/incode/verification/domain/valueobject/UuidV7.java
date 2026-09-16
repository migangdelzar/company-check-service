package com.incode.verification.domain.valueobject;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/** Small dependency-free UUIDv7 generator for internal time-sortable identifiers. */
public final class UuidV7 {
  private static final long TIMESTAMP_MASK = 0x0000FFFFFFFFFFFFL;
  private static final SecureRandom RANDOM = new SecureRandom();

  private UuidV7() {}

  public static UUID generate() {
    long timestamp = Instant.now().toEpochMilli() & TIMESTAMP_MASK;
    long mostSignificantBits = timestamp << 16;
    mostSignificantBits |= 0x7000L;
    mostSignificantBits |= RANDOM.nextInt(1 << 12);

    long leastSignificantBits = RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL;
    leastSignificantBits |= 0x8000000000000000L;
    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
