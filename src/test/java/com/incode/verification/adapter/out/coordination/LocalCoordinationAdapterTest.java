package com.incode.verification.adapter.out.coordination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.query.NormalizedQuery;
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

class LocalCoordinationAdapterTest {
  private final LocalCoordinationAdapter adapter =
      new LocalCoordinationAdapter(new ConcurrentMapCacheManager("verification"));
  private final NormalizedQuery query = new NormalizedQuery("ACME");
  private final VerificationResult result =
      new VerificationResult(null, "acme", "ACME", null, null, null, null, null, null, null);

  @Test
  void storesAndReadsResultsFromTheLocalCache() {
    assertEquals(java.util.Optional.empty(), adapter.get(query));

    adapter.put(query, result);

    assertEquals(java.util.Optional.of(result), adapter.get(query));
  }

  @Test
  void allowsOnlyOneLocalLeaseAtATime() throws Exception {
    var first = adapter.acquire(query);
    var executor = Executors.newSingleThreadExecutor();

    try {
      assertTrue(first.acquired());
      assertFalse(executor.submit(() -> adapter.acquire(query).acquired()).get());

      first.close();
      var second = adapter.acquire(query);
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
                      var lease = adapter.acquire(query);
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
