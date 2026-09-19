package com.incode.verification.repository.coordination;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.VerificationResult;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class LocalCoordinationRepositoryTest {
  private final LocalCoordinationRepository repository =
      new LocalCoordinationRepository(new ConcurrentMapCacheManager("verification"));
  private final NormalizedQuery query = new NormalizedQuery("ACME");
  private final VerificationResult result =
      new VerificationResult(null, "acme", "ACME", null, null, null, null, null, null, null);

  @Test
  void storesAndReadsResultsFromTheLocalCache() {
    assertTrue(repository.get(query).blockOptional().isEmpty());
    repository.put(query, result).block();
    assertTrue(repository.get(query).blockOptional().orElseThrow() == result);
  }

  @Test
  void allowsOnlyOneLocalLeaseAtATime() {
    var first = repository.acquire(query).block();
    assertTrue(first.acquired());
    try (var executor = Executors.newSingleThreadExecutor()) {
      var second = executor.submit(() -> repository.acquire(query).block()).get();
      assertFalse(second.acquired());
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }

    first.release().block();
    var third = repository.acquire(query).block();
    try {
      assertTrue(third.acquired());
    } finally {
      third.release().block();
    }
  }
}
