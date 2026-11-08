package it.pagopa.pn.kafka.bridge.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KafkaPaymentEventAgeCacheTest {
    private KafkaPaymentEventAgeCache cache;

    @BeforeEach
    void setUp() {
        cache = new KafkaPaymentEventAgeCache();
    }

    @Test
    void recordAge_shouldInsertNewPartition() {
        cache.recordAge(1, 100L);

        assertEquals(100L, cache.get(1));
        assertEquals(1, cache.size());
    }

    @Test
    void recordAge_shouldKeepMaximumValueForPartition() {
        cache.recordAge(1, 100L);
        cache.recordAge(1, 200L);
        cache.recordAge(1, 150L);

        assertEquals(200L, cache.get(1));
    }

    @Test
    void recordAge_shouldHandleMultiplePartitions() {
        cache.recordAge(0, 100L);
        cache.recordAge(1, 200L);
        cache.recordAge(2, 300L);

        assertEquals(3, cache.size());
        assertEquals(100L, cache.get(0));
        assertEquals(200L, cache.get(1));
        assertEquals(300L, cache.get(2));
    }

    @Test
    void drain_shouldReturnAllValuesAndEmptyCache() {
        cache.recordAge(0, 100L);
        cache.recordAge(1, 200L);
        cache.recordAge(2, 300L);

        Map<Integer, Long> drained = cache.drain();

        assertEquals(
                Map.of(0, 100L, 1, 200L, 2, 300L),
                drained
        );
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    void drain_shouldReturnEmptyMapWhenCacheIsEmpty() {
        Map<Integer, Long> drained = cache.drain();

        assertNotNull(drained);
        assertTrue(drained.isEmpty());
        assertTrue(cache.isEmpty());
    }

    @Test
    void drain_shouldAllowRecordingAfterDrain() {
        cache.recordAge(1, 100L);

        cache.drain();

        cache.recordAge(1, 200L);

        assertEquals(200L, cache.get(1));
        assertEquals(1, cache.size());
    }

    @Test
    void get_shouldReturnNullForMissingKey() {
        assertNull(cache.get(99));
    }

    @Test
    void remove_shouldRemoveAndReturnValue() {
        cache.recordAge(1, 100L);

        Long removed = cache.remove(1);

        assertEquals(100L, removed);
        assertNull(cache.get(1));
        assertTrue(cache.isEmpty());
    }

    @Test
    void remove_shouldReturnNullForMissingKey() {
        assertNull(cache.remove(99));
    }

    @Test
    void keys_shouldReturnCurrentKeys() {
        cache.recordAge(1, 100L);
        cache.recordAge(2, 200L);

        Set<Integer> keys = cache.keys();

        assertEquals(Set.of(1, 2), keys);
    }

    @Test
    void snapshot_shouldReturnCopyOfCache() {
        cache.recordAge(1, 100L);
        cache.recordAge(2, 200L);

        Map<Integer, Long> snapshot = cache.snapshot();

        assertEquals(Map.of(1, 100L, 2, 200L), snapshot);

        cache.recordAge(1, 300L);
        cache.recordAge(3, 400L);

        assertEquals(100L, snapshot.get(1));
        assertFalse(snapshot.containsKey(3));
    }

    @Test
    void snapshot_shouldBeImmutable() {
        cache.recordAge(1, 100L);

        Map<Integer, Long> snapshot = cache.snapshot();

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.put(2, 200L)
        );
    }

    @Test
    void size_shouldReturnNumberOfEntries() {
        assertEquals(0, cache.size());

        cache.recordAge(1, 100L);
        cache.recordAge(2, 200L);

        assertEquals(2, cache.size());

        cache.remove(1);

        assertEquals(1, cache.size());
    }

    @Test
    void isEmpty_shouldReturnCorrectState() {
        assertTrue(cache.isEmpty());

        cache.recordAge(1, 100L);

        assertFalse(cache.isEmpty());

        cache.remove(1);

        assertTrue(cache.isEmpty());
    }

    @Test
    void clear_shouldRemoveAllEntries() {
        cache.recordAge(1, 100L);
        cache.recordAge(2, 200L);

        cache.clear();

        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertTrue(cache.keys().isEmpty());
    }
}