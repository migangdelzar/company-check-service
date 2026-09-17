package com.incode.verification.adapter.out.observability;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class MicrometerTelemetryAdapter {
  private final MeterRegistry registry;

  public MicrometerTelemetryAdapter(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  public void operation(String operation, String outcome) {
    registry
        .counter(
            "verification.operation", "operation", bounded(operation), "outcome", bounded(outcome))
        .increment();
  }

  public void provider(String provider, String outcome) {
    registry
        .counter(
            "verification.provider", "provider", bounded(provider), "outcome", bounded(outcome))
        .increment();
  }

  public void cache(String layer, String outcome) {
    registry
        .counter("verification.cache", "cache_layer", bounded(layer), "outcome", bounded(outcome))
        .increment();
  }

  public void retry(String provider, String outcome) {
    registry
        .counter("verification.retry", "provider", bounded(provider), "outcome", bounded(outcome))
        .increment();
  }

  public void fallback(String outcome) {
    registry.counter("verification.fallback", "outcome", bounded(outcome)).increment();
  }

  public void latency(String operation, Duration duration) {
    registry
        .timer(
            "verification.latency",
            "operation",
            bounded(operation),
            "latency_class",
            latencyClass(duration))
        .record(duration);
  }

  private static String bounded(String value) {
    return value == null || value.isBlank() ? "unknown" : value.toLowerCase(java.util.Locale.ROOT);
  }

  private static String latencyClass(Duration duration) {
    long millis = duration.toMillis();
    return millis < 100
        ? "lt_100ms"
        : millis < 500 ? "100_500ms" : millis < 2000 ? "500ms_2s" : "gte_2s";
  }
}
