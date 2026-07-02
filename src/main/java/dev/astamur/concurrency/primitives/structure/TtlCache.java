package dev.astamur.concurrency.primitives.structure;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A thread-safe cache whose entries expire a fixed time-to-live (TTL) after they are written.
 *
 * <p>Two expiry strategies work together:
 *
 * <ul>
 *   <li><b>Lazy</b> — {@link #get(Object)} treats an expired entry as absent and removes it with an
 *       atomic compare-and-remove ({@link ConcurrentMap#remove(Object, Object)}), so it never
 *       clobbers a value that another thread refreshed in the meantime.
 *   <li><b>Active</b> (optional) — a background daemon thread periodically sweeps expired entries,
 *       so memory is reclaimed even for keys that are never read again.
 * </ul>
 *
 * <p>Expiry is measured with {@link System#nanoTime()} — a monotonic clock — so adjusting the
 * system wall clock cannot corrupt TTLs.
 */
public class TtlCache<K, V> implements AutoCloseable {

  private final ConcurrentMap<K, Entry<V>> map = new ConcurrentHashMap<>();
  private final long ttlNanos;
  private final ScheduledExecutorService cleaner;

  /** Creates a cache with lazy expiry only (no background thread). */
  public TtlCache(Duration ttl) {
    this(ttl, null);
  }

  /**
   * Creates a cache with lazy expiry plus active eviction that runs every {@code cleanupInterval}.
   *
   * @param cleanupInterval how often the background sweep runs; {@code null} disables active
   *     eviction
   */
  public TtlCache(Duration ttl, Duration cleanupInterval) {
    if (ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("TTL must be positive");
    }
    this.ttlNanos = ttl.toNanos();

    if (cleanupInterval == null) {
      this.cleaner = null;
      return;
    }

    this.cleaner =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "ttl-cache-cleaner");
              thread.setDaemon(true);
              return thread;
            });
    long intervalNanos = cleanupInterval.toNanos();
    cleaner.scheduleAtFixedRate(
        this::evictExpired, intervalNanos, intervalNanos, TimeUnit.NANOSECONDS);
  }

  public void put(K key, V value) {
    map.put(key, new Entry<>(value, System.nanoTime() + ttlNanos));
  }

  public V get(K key) {
    Entry<V> entry = map.get(key);
    if (entry == null) {
      return null;
    }
    if (entry.isExpired()) {
      // Compare-and-remove: only drop it if it is still the same expired entry we just read,
      // so a concurrent put(key, ...) that refreshed the value is not lost.
      map.remove(key, entry);
      return null;
    }
    return entry.value();
  }

  public boolean containsKey(K key) {
    return get(key) != null;
  }

  /** Number of entries currently held, including any that are expired but not yet swept. */
  public int size() {
    return map.size();
  }

  private void evictExpired() {
    map.values().removeIf(Entry::isExpired);
  }

  @Override
  public void close() {
    if (cleaner != null) {
      cleaner.shutdownNow();
    }
  }

  private record Entry<V>(V value, long expiresAtNanos) {
    boolean isExpired() {
      return System.nanoTime() - expiresAtNanos >= 0;
    }
  }
}
