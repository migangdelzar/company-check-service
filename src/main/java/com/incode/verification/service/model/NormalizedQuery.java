package com.incode.verification.service.model;

import com.incode.verification.exception.domain.InvalidQueryException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public record NormalizedQuery(String value) {
  public NormalizedQuery(String value) {
    String requiredValue = Objects.requireNonNull(value, "value");
    if (requiredValue.isEmpty() || requiredValue.length() > 128) {
      throw new InvalidQueryException("query must contain between 1 and 128 characters");
    }
    if (requiredValue.codePoints().anyMatch(NormalizedQuery::isUnsafeCharacter)) {
      throw new InvalidQueryException("query must contain no control characters");
    }
    this.value = requiredValue;
  }

  public static NormalizedQuery normalize(String raw) {
    Objects.requireNonNull(raw, "raw");
    var canonical = Normalizer.normalize(raw, Normalizer.Form.NFKC);
    return new NormalizedQuery(canonical.strip().toUpperCase(Locale.ROOT));
  }

  private static boolean isUnsafeCharacter(int codePoint) {
    return Character.isISOControl(codePoint) || Character.getType(codePoint) == Character.FORMAT;
  }
}
