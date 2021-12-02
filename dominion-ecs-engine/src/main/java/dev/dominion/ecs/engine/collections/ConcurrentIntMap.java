package dev.dominion.ecs.engine.collections;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class ConcurrentIntMap<V> implements SparseIntMap<V> {

    private final int[] dense;
    private final int[] sparse;
    private final Object[] values;
    private final StampedLock lock = new StampedLock();
    private final int capacity;
    private int size = 0;

    private ConcurrentIntMap(int[] dense, int[] sparse, Object[] values) {
        this.dense = dense;
        this.sparse = sparse;
        this.values = values;
        capacity = values.length;
    }

    public ConcurrentIntMap() {
        this(1 << 10);
    }

    public ConcurrentIntMap(int capacity) {
        this(
                new int[capacity],
                new int[capacity],
                new Object[capacity]
        );
    }

    @Override
    public void put(int key, V value) {
        long stamp = lock.writeLock();
        try {
            putValue(key, value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private void putValue(int key, V value) {
        Object current = get(key);
        int i = current == null ? size++ : sparse[key];
        dense[i] = key;
        sparse[key] = i;
        values[i] = value;
    }

    @Override
    public V get(int key) {
        int i = sparse[key];
        if (i > size || dense[i] != key) return null;
        return valueAt(i);
    }

    @Override
    public Boolean contains(int key) {
        int i = sparse[key];
        return i <= size && dense[i] == key;
    }

    @Override
    public V computeIfAbsent(int key, Function<Integer, ? extends V> mappingFunction) {
        V value;
        long stamp = lock.readLock();
        try {
            while ((value = get(key)) == null) {
                long ws = lock.tryConvertToWriteLock(stamp);
                if (ws != 0L) {
                    stamp = ws;
                    putValue(key, value = mappingFunction.apply(key));
                    break;
                } else {
                    lock.unlockRead(stamp);
                    stamp = lock.writeLock();
                }
            }
            return value;
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int index) {
        return (V) values[index];
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<V> iterator() {
        return new ObjectIterator<>((V[]) values, size);
    }

    @Override
    public Stream<V> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public SparseIntMap<V> clone() {
        int[] newDense = new int[capacity];
        int[] newSparse = new int[capacity];
        Object[] newValues = new Object[capacity];
        System.arraycopy(dense, 0, newDense, 0, capacity);
        System.arraycopy(sparse, 0, newSparse, 0, capacity);
        System.arraycopy(values, 0, newValues, 0, capacity);
        ConcurrentIntMap<V> cloned = new ConcurrentIntMap<>(newDense, newSparse, newValues);
        cloned.size = size;
        return cloned;
    }
}