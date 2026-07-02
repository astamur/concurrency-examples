package dev.astamur.concurrency.primitives;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import dev.astamur.concurrency.primitives.structure.TtlCache;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class TtlCacheTest {

  /**
   * Global defaults applied to every {@code await()} that does not override them:
   *
   * <ul>
   *   <li>{@code pollInterval} — how often the condition is re-checked (default 100 ms).
   *   <li>{@code pollDelay} — how long to wait before the FIRST check (default 100 ms); {@code
   *       ZERO} means "check immediately".
   *   <li>{@code timeout} — the hard deadline; a failing condition throws after this.
   * </ul>
   */
  @BeforeAll
  static void configureAwaitility() {
    Awaitility.setDefaultPollInterval(Duration.ofMillis(20));
    Awaitility.setDefaultPollDelay(Duration.ZERO);
    Awaitility.setDefaultTimeout(Duration.ofSeconds(2));
  }

  @AfterAll
  static void resetAwaitility() {
    Awaitility.reset();
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void returnsValueBeforeTtlAndNullAfter() {
    try (var cache = new TtlCache<String, String>(Duration.ofMillis(150))) {
      cache.put("k", "v");

      assertThat(cache.get("k")).isEqualTo("v");

      await().atMost(Duration.ofSeconds(1)).until(() -> cache.get("k") == null);
    }
  }

  @Test
  void backgroundEvictionEmptiesTheCache() {
    try (var cache = new TtlCache<String, String>(Duration.ofMillis(100), Duration.ofMillis(30))) {
      cache.put("a", "1");
      cache.put("b", "2");

      assertThat(cache.size()).isEqualTo(2);

      await().untilAsserted(() -> assertThat(cache.size()).isZero());
    }
  }

  @Test
  void entryStaysPresentForTheWholeTtlWindow() {
    try (var cache = new TtlCache<String, String>(Duration.ofMillis(200))) {
      cache.put("k", "v");

      await()
          .during(Duration.ofMillis(180))
          .atMost(Duration.ofSeconds(1))
          .pollInterval(Duration.ofMillis(5))
          .until(() -> cache.get("k") != null);
    }
  }
}
