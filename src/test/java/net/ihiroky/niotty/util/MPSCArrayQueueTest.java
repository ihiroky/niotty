package net.ihiroky.niotty.util;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class MPSCArrayQueueTest {

    private MPSCArrayQueue<Integer> sut_;

    @Before
    public void setUp() throws Exception {
        sut_ = new MPSCArrayQueue<Integer>(1024);
    }

    @Test
    public void testOfferAndPoll() throws Exception {
        sut_.offer(0);
        sut_.offer(1);
        sut_.offer(2);

        Integer p0 = sut_.poll();
        Integer p1 = sut_.poll();
        Integer p2 = sut_.poll();
        Integer p3 = sut_.poll();

        assertThat(p0, is(0));
        assertThat(p1, is(1));
        assertThat(p2, is(2));
        assertThat(p3, is(nullValue()));
    }

    @Test
    public void testOfferAndPollMultiThread() throws Exception {
        final int N = 100000;
        final AtomicInteger count = new AtomicInteger();
        final Integer FINISH = -1;
        Thread producer0 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < N; i += 2) {
                    sut_.offer(i);
                }
                sut_.offer(FINISH);
                System.out.println("End " + Thread.currentThread());
            }
        }, "P0");
        Thread producer1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i < N; i += 2) {
                    sut_.offer(i);
                }
                sut_.offer(FINISH);
                System.out.println("End " + Thread.currentThread());
            }
        }, "P1");
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                int finishCount = 0;
                for (;;) {
                    Integer i = sut_.poll();
                    if (i == null) {
                        continue; // may busy loop.
                    }
                    if (i != FINISH) {
                        count.incrementAndGet();
                    } else {
                        if (++finishCount == 2) {
                            break;
                        }
                    }
                }
                System.out.println("End " + Thread.currentThread());
            }
        }, "C");
        producer0.start();
        producer1.start();
        consumer.start();
        producer0.join();
        producer1.join();
        consumer.join();

        assertThat(count.get(), is(N));
    }

    @Test
    public void testIsEmptyReturnsTrueIfDoesNotHaveElements() throws Exception {
        assertThat(sut_.isEmpty(), is(true));
    }

    @Test
    public void testIsEmptyReturnsFalseIfHaveSomeElements() throws Exception {
        sut_.offer(0);

        assertThat(sut_.isEmpty(), is(false));
    }

    @Test
    public void testSize() throws Exception {
        sut_.offer(0);
        sut_.offer(0);
        sut_.offer(0);
        sut_.poll();

        assertThat(sut_.size(), is(2));
    }
}
