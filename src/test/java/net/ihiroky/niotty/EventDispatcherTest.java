package net.ihiroky.niotty;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class EventDispatcherTest {

    private EventDispatcherMock sut_;
    private static ExecutorService executor_;
    private static final String THREAD_NAME = "TEST";

    @BeforeClass
    public static void beforeClass() {
        executor_ = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, THREAD_NAME);
            }
        });
    }

    @AfterClass
    public static void afterClass() {
        executor_.shutdownNow();
    }

    @Before
    public void setUp() {
        sut_ = new EventDispatcherMock();
    }

    @After
    public void tearDown() throws Exception {
        sut_.close();
        while (sut_.isAlive()) {
            Thread.sleep(10);
        }
    }

    @Test(timeout = 1000)
    public void testOfferEvent() throws Exception {
        executor_.execute(sut_);
        final boolean[] executed = new boolean[1];
        @SuppressWarnings("unchecked")
        Event t = mock(Event.class);
        doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                executed[0] = true;
                return Event.RETRY_IMMEDIATELY;
            }
        }).when(t).execute(TimeUnit.NANOSECONDS);

        sut_.offer(t);

        while (!executed[0]) {
            Thread.sleep(10);
        }
    }

    @Test//(timeout = 1000)
    public void testProcessEvent_ExecuteAgainLater() throws Exception {
        executor_.execute(sut_);
        final AtomicInteger counter = new AtomicInteger();

        @SuppressWarnings("unchecked")
        Event t = mock(Event.class);
        doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                counter.getAndIncrement();
                return 10L;
            }
        }).when(t).execute(TimeUnit.NANOSECONDS);

        sut_.offer(t);

        // t is retried forever, so check until 10
        while (counter.get() < 10) {
            Thread.sleep(10);
        }
    }

    @Test(timeout = 1000)
    public void testIsInDispatcherThread_ReturnsTrueIfInEventDispatcher() throws Exception {
        executor_.execute(sut_);
        final boolean[] isInDispatcherThread = new boolean[1];
        @SuppressWarnings("unchecked")
        Event t = mock(Event.class);
        doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                isInDispatcherThread[0] = sut_.isInDispatcherThread();
                return Event.RETRY_IMMEDIATELY;
            }
        }).when(t).execute(TimeUnit.NANOSECONDS);

        sut_.offer(t);

        while (!isInDispatcherThread[0]) {
            Thread.sleep(10);
        }
    }

    @Test
    public void testIsInDispatcherThread_ReturnsFalseIfNotInEventDispatcher() throws Exception {
        assertThat(sut_.isInDispatcherThread(), is(false));
    }

    @Test(timeout = 1000)
    public void testIsAlive_ReturnsTrueIfSutIsExecuted() throws Exception {
        executor_.execute(sut_);

        while (!sut_.isAlive()) {
            Thread.sleep(10);
        }
    }

    @Test
    public void testIsAlive_ReturnsFalseIfSutIsNotExecuted() throws Exception {
        assertThat(sut_.isAlive(), is(false));
    }

    @Test
    public void testToString_ReturnsThreadNameIfSutIsExecuted() throws Exception {
        executor_.execute(sut_);

        while (!sut_.toString().contains(THREAD_NAME)) {
            Thread.sleep(10);
        }
    }

    @Test
    public void testToString_ReturnsDefaultIfSutIsNotExecuted() throws Exception {
        String actual = sut_.toString();
        assertThat(actual, actual.matches(".*\\.EventDispatcherMock@[0-9a-f]+"), is(true));
    }

    @Test
    public void testCompareTo_ReturnsPositiveIfSutHasMoreSelection() throws Exception {
        EventDispatcher t = new EventDispatcherMock();
        EventDispatcherSelection s0 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection s1 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection s2 = mock(EventDispatcherSelection.class);

        t.accept(s0);
        sut_.accept(s1);
        sut_.accept(s2);


        assertThat(sut_.compareTo(t) > 0, is(true));
    }

    @Test
    public void testCompareTo_ReturnsNegativeIfSutHasLessSelection() throws Exception {
        EventDispatcher t = new EventDispatcherMock();
        EventDispatcherSelection s0 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection s1 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection s2 = mock(EventDispatcherSelection.class);

        t.accept(s0);
        t.accept(s1);
        sut_.accept(s2);

        assertThat(sut_.compareTo(t) < 0, is(true));
    }

    @Test
    public void testCompareTo_Return0IfSelectionCountIsTheSame() throws Exception {
        EventDispatcher t = new EventDispatcherMock();
        EventDispatcherSelection s0 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection s1 = mock(EventDispatcherSelection.class);

        t.accept(s0);
        sut_.accept(s1);

        assertThat(sut_.compareTo(t), is(0));
    }

    @Test
    public void testAcceptReject_Ordinary() throws Exception {
        EventDispatcherSelection s0 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection s1 = mock(EventDispatcherSelection.class);

        assertThat(sut_.selectionCount(), is(0));
        assertThat(sut_.accept(s0), is(1));
        assertThat(sut_.accept(s1), is(2));
        assertThat(sut_.selectionCount(), is(2));
        assertThat(sut_.reject(s0), is(1));
        assertThat(sut_.reject(s1), is(0));
        assertThat(sut_.selectionCount(), is(0));
    }

    @Test(timeout = 1000)
    public void testRun_DelayQueueIsPolledTwiceSimultaneously() throws Exception {
        executor_.execute(sut_);
        final boolean[] done = new boolean[2];
        Event event0 = mock(Event.class);
        when(event0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[0] = true;
                return Event.DONE;
            }
        });
        Event event1 = mock(Event.class);
        when(event1.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[1] = true;
                return Event.DONE;
            }
        });

        EventFuture e0 = sut_.schedule(event0, 100, TimeUnit.MILLISECONDS);
        EventFuture e1 = sut_.schedule(event1, 100, TimeUnit.MILLISECONDS);

        while (!done[0] || !done[1]) {
            Thread.sleep(10);
        }
        verify(event0, timeout(120)).execute(TimeUnit.NANOSECONDS);
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        verify(event1, timeout(120)).execute(TimeUnit.NANOSECONDS);
        assertThat(e1.isDispatched(), is(true));
        assertThat(e1.isCancelled(), is(false));
    }

    @Test(timeout = 1000)
    public void testRun_DelayQueuePriority() throws Exception {
        executor_.execute(sut_);
        final Queue<Integer> order = new ArrayBlockingQueue<Integer>(10);
        Event event0 = mock(Event.class);
        when(event0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(0);
                return Event.DONE;
            }
        });
        when(event0.toString()).thenReturn("event0");
        Event event1 = mock(Event.class);
        when(event1.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(1);
                return Event.DONE;
            }
        });
        when(event1.toString()).thenReturn("event1");

        EventFuture e0 = sut_.schedule(event0, 200, TimeUnit.MILLISECONDS);
        EventFuture e1 = sut_.schedule(event1, 100, TimeUnit.MILLISECONDS);

        while (order.size() < 2) {
            Thread.sleep(10);
        }
        assertThat(order.poll(), is(1));
        assertThat(order.poll(), is(0));
        verify(event0, timeout(220)).execute(TimeUnit.NANOSECONDS);
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        verify(event1, timeout(120)).execute(TimeUnit.NANOSECONDS);
        assertThat(e1.isDispatched(), is(true));
        assertThat(e1.isCancelled(), is(false));
    }

    @Test
    public void testEventFutureCancel_RemoveHeadEntryInDelayQueue() throws Exception {
        executor_.execute(sut_);
        Event event0 = mock(Event.class);
        when(event0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                return Event.DONE;
            }
        });

        EventFuture e = sut_.schedule(event0, 100, TimeUnit.MILLISECONDS);
        e.cancel();
        while (!e.isDone()) {
            Thread.sleep(10);
        }
        assertThat(e.isDispatched(), is(false));
        assertThat(e.isCancelled(), is(true));
    }

    @Test
    public void testEventFutureCancel_RemoveHeadEntryInDelayQueueAfterExecute() throws Exception {
        executor_.execute(sut_);
        Event event0 = mock(Event.class);
        when(event0.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                // System.out.println("event0");
                return Event.DONE;
            }
        });
        Event event1 = mock(Event.class);
        when(event1.execute(TimeUnit.NANOSECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                // System.out.println("event1");
                return Event.DONE;
            }
        });

        EventFuture e0 = sut_.schedule(event0, 100, TimeUnit.MILLISECONDS);
        EventFuture e1 = sut_.schedule(event1, 100, TimeUnit.MILLISECONDS);
        e1.cancel();
        while (!e0.isDone() || !e1.isDone()) {
            // System.out.println(e0 + ", " + e1);
            Thread.sleep(10);
        }
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        assertThat(e1.isDispatched(), is(false));
        assertThat(e1.isCancelled(), is(true));
    }

}
