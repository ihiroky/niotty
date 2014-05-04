package net.ihiroky.niotty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ThreadFactory;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class EventDispatcherGroupTest {

    private EventDispatcherGroupMock sut_;
    private ThreadFactory threadFactory_;

    @Before
    public void setUp() {
        threadFactory_ = new NameCountThreadFactory("EventDispatcherGroupTest");
    }

    @After
    public void tearDown() {
        if (sut_ != null) {
            sut_.close();
        }
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsOpen() throws Exception {
        sut_ = new EventDispatcherGroupMock(threadFactory_, 1);
        EventDispatcherSelection ts = mock(EventDispatcherSelection.class);
        sut_.assign(ts);
        assertThat(sut_.isOpen(), is(true));
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsNotOpen() throws Exception {
        sut_ = new EventDispatcherGroupMock(threadFactory_, 1);
        sut_.close();
        assertThat(sut_.isOpen(), is(false));
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsReopen() throws Exception {
        sut_ = new EventDispatcherGroupMock(threadFactory_, 1);
        sut_.close();
        sut_.open();

        assertThat(sut_.isOpen(), is(true));
    }

    @Test
    public void testAssign_ReturnsMinimumSelectedEventDispatcher() throws Exception {
        sut_ = new EventDispatcherGroupMock(threadFactory_, 3);
        EventDispatcherSelection ts1 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection ts2 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection ts3 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection ts = mock(EventDispatcherSelection.class);

        EventDispatcherMock t1 = sut_.assign(ts1);
        EventDispatcherMock t2 = sut_.assign(ts2);
        EventDispatcherMock t3 = sut_.assign(ts3);
        EventDispatcherMock t = sut_.assign(ts);

        assertThat(t1.selectionCount(), is(2)); // 1 + 3
        assertThat(t2.selectionCount(), is(1));
        assertThat(t3.selectionCount(), is(1));
        assertThat(t, is(sameInstance(t1)));
        assertThat(t, is(not(sameInstance(t2))));
        assertThat(t, is(not(sameInstance(t3))));
    }

    @Test(expected = NullPointerException.class)
    public void testAssign_ExceptionIfSelectionIsNull() throws Exception {
        sut_ = new EventDispatcherGroupMock(threadFactory_, 1);
        sut_.assign(null);
    }

    @Test(timeout = 1000)
    public void testAssign_SweepDeadDispatcher() throws Exception {
        sut_ = new EventDispatcherGroupMock(threadFactory_, 3);
        EventDispatcherSelection ts1 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection ts2 = mock(EventDispatcherSelection.class);
        EventDispatcherSelection ts3 = mock(EventDispatcherSelection.class);

        EventDispatcherMock t1 = sut_.assign(ts1);
        EventDispatcherMock t2 = sut_.assign(ts2);
        EventDispatcherMock t3 = sut_.assign(ts3);
        t2.close();
        while (t2.selectionCount() > 0) {
            Thread.sleep(10);
        }
        EventDispatcherMock t = sut_.assign(ts2);

        assertThat(t, is(not(sameInstance(t1))));
        assertThat(t, is(not(sameInstance(t2))));
        assertThat(t, is(not(sameInstance(t3))));
    }

    @Test
    public void testAssign_ReturnsTheSameEventDispatcherForTheSameSelection() throws Exception {
        sut_ = new EventDispatcherGroupMock(threadFactory_, 3);
        EventDispatcherSelection ts1 = mock(EventDispatcherSelection.class);

        EventDispatcherMock t0 = sut_.assign(ts1);
        EventDispatcherMock t1 = sut_.assign(ts1);

        assertThat(t0, is(sameInstance(t1)));
        assertThat(t0.selectionCount(), is(1));
    }

    private static class EventDispatcherGroupMock extends EventDispatcherGroup<EventDispatcherMock> {

        /**
         * Constructs a new instance.
         *
         * @param threadFactory a factory to create thread which runs a event dispatcher
         * @param workers       the number of threads held in the thread pool
         */
        protected EventDispatcherGroupMock(ThreadFactory threadFactory, int workers) {
            super(workers, threadFactory, new EventDispatcherFactory<EventDispatcherMock>() {
                @Override
                public EventDispatcherMock newEventDispatcher() {
                    return new EventDispatcherMock();
                }
            });
        }
    }
}
