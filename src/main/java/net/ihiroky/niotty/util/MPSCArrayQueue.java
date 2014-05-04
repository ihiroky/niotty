package net.ihiroky.niotty.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;

/**
 * A size limited array based queue which supports multiple producers and
 * single consumer, multiple consumers are not supported.
 *
 * This class is not a general-purpose Queue implementation.
 * The {@link #offer(Object)} may block if no space to insert is available.
 * So {@link #offer(Object)} always returns true. And some methods of
 * {@link java.util.Queue} throws {@link java.lang.UnsupportedOperationException}.
 *
 * @param <E> the type of the element in this queue
 */
public final class MPSCArrayQueue<E> implements Queue<E> {

    private final int MASK;
    private final E[] BUFFER;

    @SuppressWarnings("unused")
    private volatile int head_;

    @SuppressWarnings("unused")
    private volatile int tail_;

    private static final long HEAD_OFFSET;
    private static final long TAIL_OFFSET;
    private static final int BUFFER_BASE;
    private static final int BUFFER_SCALE;
    private static final int MAX_SPIN_COUNT_ON_FULL = 1000;
    private static final long EXPECTED_PARK_NANOS_ON_FULL = 1L;

    static {
        try {
            HEAD_OFFSET = Platform.UNSAFE.objectFieldOffset(MPSCArrayQueue.class.getDeclaredField("head_"));
            TAIL_OFFSET = Platform.UNSAFE.objectFieldOffset(MPSCArrayQueue.class.getDeclaredField("tail_"));
            BUFFER_BASE = Platform.UNSAFE.arrayBaseOffset(Object[].class);
            BUFFER_SCALE = Platform.UNSAFE.arrayIndexScale(Object[].class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Constructs an new instance with the given (fixed) capacity.
     * The capacity is round up to power of two.
     *
     * @param capacity the capacity
     */
    @SuppressWarnings("unchecked")
    public MPSCArrayQueue(int capacity)
    {
        capacity = findNextPositivePowerOfTwo(capacity);
        MASK = capacity - 1;
        BUFFER = (E[]) new Object[capacity];
    }

    private static int findNextPositivePowerOfTwo(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    private static long bufferOffset(int i) {
        return BUFFER_BASE + i * BUFFER_SCALE;
    }

    @Override
    public boolean add(final E e) {
        if (offer(e)) {
            return true;
        }
        throw new IllegalStateException("Queue is full");
    }

    @Override
    public boolean offer(final E e) {
        if (e == null) {
            throw new NullPointerException("Null is not a valid element");
        }

        int currentTail;
        for (int count = 0; ; count++) {
            currentTail = tail_;
            final int size = (currentTail - head_) & MASK;
            if (size == MASK) { // Can't check null without accessing to the BUFFER.
                if (++count == MAX_SPIN_COUNT_ON_FULL) {
                    LockSupport.parkNanos(this, EXPECTED_PARK_NANOS_ON_FULL);
                }
                continue;
            }
            if (Platform.UNSAFE.compareAndSwapInt(this, TAIL_OFFSET, currentTail, (currentTail + 1) & MASK)) {
                break;
            }
        }
        Platform.UNSAFE.putOrderedObject(BUFFER, bufferOffset(currentTail), e);
        return true;
    }

    @Override
    public E poll() {
        final int currentHead = head_;
        final long offset = bufferOffset(currentHead);
        final E e = (E) Platform.UNSAFE.getObjectVolatile(BUFFER, offset);
        if (e != null) {
            Platform.UNSAFE.putOrderedObject(BUFFER, offset, null);
            Platform.UNSAFE.putOrderedInt(this, HEAD_OFFSET, (currentHead + 1) & MASK);
        }
        return e;
    }

    @Override
    public E remove() {
        final E e = poll();
        if (e == null) {
            throw new NoSuchElementException("Queue is empty");
        }
        return e;
    }

    @Override
    public E element() {
        final E e = peek();
        if (e == null) {
            throw new NoSuchElementException("Queue is empty");
        }
        return e;
    }

    @Override
    public E peek() {
        return (E) Platform.UNSAFE.getObjectVolatile(BUFFER, bufferOffset(head_));
    }

    @Override
    public int size() {
        return (tail_ - head_) & MASK;
    }

    @Override
    public boolean isEmpty() {
        return tail_ == head_;
    }

    @Override
    public boolean contains(final Object o) {
        if (o == null) {
            return false;
        }

        for (int i = head_, limit = tail_; i < limit; i++) {
            final E e = (E) Platform.UNSAFE.getObjectVolatile(BUFFER, bufferOffset(i));
            if (o.equals(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Throws UnsupportedOperationException.
     * @return nothing
     */
    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException.
     * @return nothing
     */
    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException.
     * @return nothing
     */
    @Override
    public <T> T[] toArray(final T[] a) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException.
     * @return nothing
     */
    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        for (final E e : c) {
            add(e);
        }

        return true;
    }

    /**
     * Throws UnsupportedOperationException.
     * @return nothing
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException.
     * @return nothing
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        Object value;
        do {
            value = poll();
        } while (value != null);
    }
}

