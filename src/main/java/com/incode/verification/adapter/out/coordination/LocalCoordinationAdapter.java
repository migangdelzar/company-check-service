package com.incode.verification.adapter.out.coordination;

import com.incode.verification.application.port.out.CoordinationPort;
import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.query.NormalizedQuery;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public final class LocalCoordinationAdapter implements CoordinationPort {
  private static final String CACHE_NAME = "verification";

  private final CacheManager cacheManager;
  private final ConcurrentMap<String, LeaseState> leases = new ConcurrentHashMap<>();

  public LocalCoordinationAdapter(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Override
  public Optional<VerificationResult> get(NormalizedQuery query) {
    var cache = cache();
    if (cache == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(cache.get(query.value(), VerificationResult.class));
  }

  @Override
  public VerificationResult put(NormalizedQuery query, VerificationResult result) {
    var cache = cache();
    if (cache != null) {
      cache.put(query.value(), result);
    }
    return result;
  }

  @Override
  public Lease acquire(NormalizedQuery query) {
    var key = query.value();
    var state = register(key);
    if (!state.lock.tryLock()) {
      release(key, state);
      return new LocalLease(key, state, false, false);
    }
    return new LocalLease(key, state, true, true);
  }

  private LeaseState register(String key) {
    return leases.compute(
        key,
        (ignored, current) -> {
          var state = current == null ? new LeaseState() : current;
          state.references++;
          return state;
        });
  }

  private @Nullable Cache cache() {
    return cacheManager.getCache(CACHE_NAME);
  }

  private void release(String key, LeaseState state) {
    leases.computeIfPresent(
        key,
        (ignored, current) -> {
          if (current != state) {
            return current;
          }
          state.references--;
          return state.references == 0 ? null : state;
        });
  }

  private void releaseLease(String key, LeaseState state) {
    leases.computeIfPresent(
        key,
        (ignored, current) -> {
          if (current != state) {
            return current;
          }
          // Keep unlock and final-reference removal atomic with the next handoff.
          state.lock.unlock();
          state.references--;
          return state.references == 0 ? null : state;
        });
  }

  private static final class LeaseState {
    private final ReentrantLock lock = new ReentrantLock();
    private int references;
  }

  private final class LocalLease implements Lease {
    private final String key;
    private final LeaseState state;
    private final boolean acquired;
    private final boolean registered;
    private final AtomicBoolean closed = new AtomicBoolean();

    private LocalLease(String key, LeaseState state, boolean acquired, boolean registered) {
      this.key = key;
      this.state = state;
      this.acquired = acquired;
      this.registered = registered;
    }

    @Override
    public boolean acquired() {
      return acquired;
    }

    @Override
    public void close() {
      if (!registered || !closed.compareAndSet(false, true)) {
        return;
      }
      releaseLease(key, state);
    }
  }
}
