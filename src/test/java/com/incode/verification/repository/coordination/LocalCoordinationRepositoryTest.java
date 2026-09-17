package com.incode.verification.repository.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.VerificationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    assertEquals(java.util.Optional.empty(), repository.get(query));

    repository.put(query, result);

    assertEquals(java.util.Optional.of(result), repository.get(query));
  }

  @Test
  void allowsOnlyOneLocalLeaseAtATime() throws Exception {
    var first = repository.acquire(query);
    var executor = Executors.newSingleThreadExecutor();

    try {
      assertTrue(first.acquired());
      assertFalse(executor.submit(() -> repository.acquire(query).acquired()).get());

      first.close();
      var second = repository.acquire(query);
      try {
        assertTrue(second.acquired());
      } finally {
        second.close();
      }
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void doesNotCreateDuplicateLeasesDuringConcurrentHandoffs() throws Exception {
    int workers = 8;
    int rounds = 2_000;
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(workers);
    var activeLeases = new AtomicInteger();
    var duplicateLease = new AtomicBoolean();
    ExecutorService executor = Executors.newFixedThreadPool(workers);
    List<Future<?>> tasks = new ArrayList<>();

    try {
      for (int worker = 0; worker < workers; worker++) {
        tasks.add(
            executor.submit(
                () -> {
                  try {
                    start.await();
                    for (int round = 0; round < rounds; round++) {
                      var lease = repository.acquire(query);
                      if (!lease.acquired()) {
                        continue;
                      }
                      if (activeLeases.incrementAndGet() != 1) {
                        duplicateLease.set(true);
                      }
                      Thread.yield();
                      activeLeases.decrementAndGet();
                      lease.close();
                    }
                  } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                  } finally {
                    done.countDown();
                  }
                }));
      }

      start.countDown();
      assertTrue(done.await(10, TimeUnit.SECONDS));
      for (var task : tasks) {
        task.get();
      }
      assertFalse(duplicateLease.get());
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }
}
