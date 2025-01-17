package dev.dominion.ecs.test.engine.collections;

import dev.dominion.ecs.engine.collections.ChunkedPool;
import dev.dominion.ecs.engine.collections.ChunkedPool.Item;
import dev.dominion.ecs.engine.system.Config;
import dev.dominion.ecs.engine.system.Logging;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class ChunkedPoolTest {
    private static final ChunkedPool.IdSchema ID_SCHEMA =
            new ChunkedPool.IdSchema(Config.DominionSize.MEDIUM.chunkBit());

    @Test
    public void newTenant() {
        try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.TEST)) {
            ChunkedPool.Tenant<TestEntity> tenant = chunkedPool.newTenant();
            Assertions.assertNotNull(tenant);
        }
    }

    @Test
    public void register() {
        try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.TEST)) {
            ChunkedPool.Tenant<TestEntity> tenant = chunkedPool.newTenant();
            TestEntity entry = new TestEntity(1, null, null);
            Assertions.assertEquals(entry, tenant.register(entry, null));
            Assertions.assertEquals(entry, chunkedPool.getEntry(1));
        }
    }

    public record TestEntity(int id, Item prev, Item next) implements Item {
        @Override
        public int getId() {
            return id;
        }

        @Override
        public void setId(int id) {
        }

        @Override
        public void setStateId(int id) {
        }

        @Override
        public ChunkedPool.LinkedChunk<? extends Item> getChunk() {
            return null;
        }

        @Override
        public void setChunk(ChunkedPool.LinkedChunk<? extends Item> chunk) {
        }

        @Override
        public void setStateChunk(ChunkedPool.LinkedChunk<? extends Item> chunk) {
        }
    }

    @Nested
    public class TenantTest {

        @Test
        public void nextId() {
            try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.STRESS_TEST)) {
                ChunkedPool.Tenant<TestEntity> tenant = chunkedPool.newTenant();
                Assertions.assertEquals(0, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(1, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(0, (tenant.nextId() >> ID_SCHEMA.chunkBit())
                        & ID_SCHEMA.chunkIdBitMask());
                for (int i = 0; i < ID_SCHEMA.chunkCapacity(); i++) {
                    tenant.nextId();
                }
                Assertions.assertEquals(1, (tenant.nextId() >> ID_SCHEMA.chunkBit())
                        & ID_SCHEMA.chunkIdBitMask());
                Assertions.assertEquals(4, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
            }
        }

        @Test
        public void freeIdAndStateId() {
            try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.STRESS_TEST)) {
                ChunkedPool.Tenant<TestEntity> tenant = chunkedPool.newTenant();
                Assertions.assertEquals(0, tenant.currentChunkSize());
                Assertions.assertEquals(0, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(1, tenant.currentChunkSize());
                Assertions.assertEquals(1, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(2, tenant.currentChunkSize()); // ready nextId == 2
                Assertions.assertEquals(1, tenant.freeId(0) & ID_SCHEMA.objectIdBitMask()); // 1 -> 0 : ready nextId == 1
                Assertions.assertEquals(1, tenant.currentChunkSize());
                Assertions.assertEquals(1, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(2, tenant.currentChunkSize());
                Assertions.assertEquals(2, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                // move to the next chunk
                for (int i = 0; i < ID_SCHEMA.chunkCapacity(); i++) {
                    tenant.nextId();
                }
                Assertions.assertEquals(3, tenant.currentChunkSize());
                Assertions.assertEquals(3, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(ID_SCHEMA.chunkCapacity() - 1, tenant.freeStateId(1) & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(4, tenant.currentChunkSize());
                Assertions.assertEquals(ID_SCHEMA.chunkCapacity() - 1, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(4, tenant.nextId() & ID_SCHEMA.objectIdBitMask());
                Assertions.assertEquals(5, tenant.currentChunkSize());
            }
        }

        @Test
        public void concurrentNextId() throws InterruptedException {
            try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.STRESS_TEST)) {
                ChunkedPool.Tenant<TestEntity> tenant = chunkedPool.newTenant();
                int capacity = 1 << 20;
                ExecutorService pool = Executors.newFixedThreadPool(16);
                int[] array = new int[capacity];
                for (int i = 0; i < capacity; i++) {
                    pool.execute(() -> {
                        int nextId = tenant.nextId();
                        array[nextId] = nextId;
                    });
                }
                pool.shutdown();
                Assertions.assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
                int actual = tenant.nextId();
                Assertions.assertEquals(capacity, actual);
                Assertions.assertEquals(actual + 1, chunkedPool.size());
                int last = -1;
                for (int id : array) {
                    Assertions.assertEquals(last, id - 1);
                    last = id;
                }
            }
        }

        @Test
        public void concurrentNextAndFreeId() throws InterruptedException {
            try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.TEST)) {
                ChunkedPool.Tenant<TestEntity> tenant = chunkedPool.newTenant();
                final int capacity = 1 << 16;
                final ExecutorService pool = Executors.newFixedThreadPool(8);
                int added = 0;
                int removed = 0;
                int[] array = new int[capacity];
                for (int i = 0; i < capacity; i++) {
                    if (i % 10 == 0) {
                        final int idx = (int) (i * 0.1);
                        pool.execute(() -> tenant.freeId(idx));
                        removed++;
                    }
                    pool.execute(() -> {
                        int nextId = tenant.nextId();
                        array[nextId] = nextId;
                    });
                    added++;
                }
                pool.shutdown();
                Assertions.assertTrue(pool.awaitTermination(600, TimeUnit.SECONDS));
                Assertions.assertEquals(added - removed, chunkedPool.size() - 1);
                Assertions.assertTrue(tenant.nextId() >= added - removed);

                int last = -1;
                for (int i = 0; i < added - removed; i++) {
                    int id = array[i];
                    Assertions.assertEquals(last, id - 1);
                    last = id;
                }
            }
        }

        @Test
        void concurrentTenants() throws InterruptedException {
            int capacity = 1 << 18;
            int threadCount = 8;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.STRESS_TEST)) {
                final ChunkedPool.Tenant<TestEntity> tenant1 = chunkedPool.newTenant(1, null, 1);
                final ChunkedPool.Tenant<TestEntity> tenant2 = chunkedPool.newTenant(2, null, 2);
                final ChunkedPool.Tenant<TestEntity> tenant3 = chunkedPool.newTenant(3, null, 3);

                for (int i = 0; i < capacity; i++) {
                    executorService.execute(tenant1::nextId);
                    if (i % 10 == 0) {
                        final int idx = (int) (i * 0.7);
                        executorService.execute(() -> tenant1.freeId(idx, false, false));
                    }
                    executorService.execute(tenant2::nextId);
                    if (i % 10 == 0) {
                        final int idx = (int) (i * 0.7);
                        executorService.execute(() -> tenant2.freeId(idx, false, false));
                    }
                    executorService.execute(tenant3::nextId);
                    if (i % 10 == 0) {
                        final int idx = (int) (i * 0.7);
                        executorService.execute(() -> tenant3.freeId(idx, false, false));
                    }
                }
                executorService.shutdown();
                Assertions.assertTrue(executorService.awaitTermination(60, TimeUnit.SECONDS));
                Assertions.assertEquals(tenant1.getDataLength(), tenant1.currentChunkLength());
                Assertions.assertEquals(tenant2.getDataLength(), tenant2.currentChunkLength());
                Assertions.assertEquals(tenant3.getDataLength(), tenant3.currentChunkLength());
            }
        }

        @Test
        public void iterator() {
            try (ChunkedPool<TestEntity> chunkedPool = new ChunkedPool<>(ID_SCHEMA, Logging.Context.STRESS_TEST)) {
                ChunkedPool.Tenant<TestEntity> tenant = chunkedPool.newTenant();
                Assertions.assertEquals(0, tenant.currentChunkSize());
                Iterator<TestEntity> iterator = tenant.iterator();
                Assertions.assertFalse(iterator.hasNext());
                int capacity = 1 << 20;
                for (int i = 0; i < capacity; i++) {
                    tenant.register(new TestEntity(tenant.nextId(), null, null), null);
                }
                iterator = tenant.iterator();
                long lastId = 0;
                while (iterator.hasNext()) {
                    long id = iterator.next().id;
                    Assertions.assertTrue((id + 1) % ID_SCHEMA.chunkCapacity() == 0 || lastId - 1 == id);
                    lastId = id;
                }
            }
        }
    }

    @Nested
    public class LinkedChunkTest {

        @Test
        public void size() {
            ChunkedPool.LinkedChunk<TestEntity> chunk =
                    new ChunkedPool.LinkedChunk<>(0, ID_SCHEMA, null, 0, null, Logging.Context.TEST);
            Assertions.assertEquals(0, chunk.incrementIndex());
            Assertions.assertEquals(1, chunk.incrementIndex());
        }

        @Test
        public void capacity() {
            ChunkedPool.LinkedChunk<TestEntity> chunk =
                    new ChunkedPool.LinkedChunk<>(0, ID_SCHEMA, null, 0, null, Logging.Context.TEST);
            Assertions.assertTrue(chunk.hasCapacity());
            for (int i = 0; i < ID_SCHEMA.chunkCapacity() - 1; i++) {
                chunk.incrementIndex();
            }
            Assertions.assertTrue(chunk.hasCapacity());
            chunk.incrementIndex();
            Assertions.assertFalse(chunk.hasCapacity());
        }

        @Test
        public void data() {
            ChunkedPool.LinkedChunk<TestEntity> previous =
                    new ChunkedPool.LinkedChunk<>(0, ID_SCHEMA, null, 0, null, Logging.Context.TEST);
            ChunkedPool.LinkedChunk<TestEntity> chunk =
                    new ChunkedPool.LinkedChunk<>(0, ID_SCHEMA, previous, 0, null, Logging.Context.TEST);
            var entity = new TestEntity(1, null, null);
            chunk.set(entity, null);
            Assertions.assertEquals(entity, chunk.get(1));
            Assertions.assertEquals(previous, chunk.getPrevious());
        }
    }
}
