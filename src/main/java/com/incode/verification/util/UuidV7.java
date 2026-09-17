package com.incode.verification.util;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/** RFC 9562 UUIDv7 generator for internal time-sortable identifiers. */
public final class UuidV7 {
  private UuidV7() {}

  public static UUID generate() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
