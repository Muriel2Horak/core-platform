package cz.muriel.core.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UUIDv7Generator
 */
class UUIDv7GeneratorTest {

  @Test
  void testGenerateProducesVersion7UUID() {
    UUID uuid = UUIDv7Generator.generate();

    assertNotNull(uuid);
    assertEquals(7, uuid.version(), "UUID should be version 7");
  }

  @Test
  void testGeneratedUUIDsAreUnique() {
    Set<UUID> uuids = new HashSet<>();
    int count = 10000;

    for (int i = 0; i < count; i++) {
      UUID uuid = UUIDv7Generator.generate();
      assertTrue(uuids.add(uuid), "Generated UUID should be unique: " + uuid);
    }

    assertEquals(count, uuids.size(), "All generated UUIDs should be unique");
  }

  @Test
  void testUUIDsAreSortableByTime() {
    UUID uuid1 = UUIDv7Generator.generate();

    // Wait a bit
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    UUID uuid2 = UUIDv7Generator.generate();

    // UUID v7 should be sortable by creation time
    assertTrue(uuid1.compareTo(uuid2) < 0, "Earlier UUID should be less than later UUID");
  }

  @Test
  void testGetTimestampExtractsCorrectTime() {
    Instant now = Instant.now();
    UUID uuid = UUIDv7Generator.generate(now);

    Instant extracted = UUIDv7Generator.getTimestamp(uuid);

    // Should be within 1ms (timestamp has millisecond precision)
    Duration diff = Duration.between(now, extracted);
    assertTrue(diff.abs().toMillis() <= 1, "Extracted timestamp should match original within 1ms");
  }

  @Test
  void testGetTimestampThrowsForNonV7UUID() {
    UUID randomUUID = UUID.randomUUID(); // This is version 4

    assertThrows(IllegalArgumentException.class, () -> UUIDv7Generator.getTimestamp(randomUUID),
        "Should throw exception for non-v7 UUID");
  }

  @Test
  void testIsUUIDv7() {
    UUID v7uuid = UUIDv7Generator.generate();
    UUID v4uuid = UUID.randomUUID();

    assertTrue(UUIDv7Generator.isUUIDv7(v7uuid), "Should recognize v7 UUID");
    assertFalse(UUIDv7Generator.isUUIDv7(v4uuid), "Should reject v4 UUID");
    assertFalse(UUIDv7Generator.isUUIDv7(null), "Should handle null");
  }

  @Test
  void testGenerateWithSpecificTimestamp() {
    Instant timestamp = Instant.parse("2025-10-10T12:00:00Z");
    UUID uuid = UUIDv7Generator.generate(timestamp);

    assertNotNull(uuid);
    assertEquals(7, uuid.version());

    Instant extracted = UUIDv7Generator.getTimestamp(uuid);
    assertEquals(timestamp, extracted, "Timestamp should match");
  }

  @Test
  void testParallelGeneration() throws InterruptedException {
    int threadCount = 10;
    int uuidsPerThread = 1000;
    Set<UUID> allUuids = new HashSet<>();
    Thread[] threads = new Thread[threadCount];

    for (int i = 0; i < threadCount; i++) {
      threads[i] = new Thread(() -> {
        Set<UUID> threadUuids = new HashSet<>();
        for (int j = 0; j < uuidsPerThread; j++) {
          threadUuids.add(UUIDv7Generator.generate());
        }
        synchronized (allUuids) {
          allUuids.addAll(threadUuids);
        }
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    assertEquals(threadCount * uuidsPerThread, allUuids.size(),
        "All UUIDs generated in parallel should be unique");
  }

  @Test
  void testUUIDNeverRepeatsAcrossMultipleCalls() {
    Set<UUID> batch1 = new HashSet<>();
    Set<UUID> batch2 = new HashSet<>();

    // Generate first batch
    for (int i = 0; i < 1000; i++) {
      batch1.add(UUIDv7Generator.generate());
    }

    // Small delay
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Generate second batch
    for (int i = 0; i < 1000; i++) {
      batch2.add(UUIDv7Generator.generate());
    }

    // No intersection
    batch1.retainAll(batch2);
    assertTrue(batch1.isEmpty(), "No UUIDs should repeat across batches");
  }
}
