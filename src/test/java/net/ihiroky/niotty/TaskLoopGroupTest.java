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
public class TaskLoopGroupTest {

    private TaskLoopGroupMock sut_;
    private ThreadFactory threadFactory_;

    @Before
    public void setUp() {
        threadFactory_ = new NameCountThreadFactory("TaskLoopGroupTest");
    }

    @After
    public void tearDown() {
        if (sut_ != null) {
            sut_.close();
        }
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsOpen() throws Exception {
        sut_ = new TaskLoopGroupMock(threadFactory_, 1);
        TaskSelection ts = mock(TaskSelection.class);
        sut_.assign(ts);
        assertThat(sut_.isOpen(), is(true));
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsNotOpen() throws Exception {
        sut_ = new TaskLoopGroupMock(threadFactory_, 1);
        sut_.close();
        assertThat(sut_.isOpen(), is(false));
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsReopen() throws Exception {
        sut_ = new TaskLoopGroupMock(threadFactory_, 1);
        sut_.close();
        sut_.open();

        assertThat(sut_.isOpen(), is(true));
    }

    @Test
    public void testAssign_ReturnsMinimumSelectedTaskLoop() throws Exception {
        sut_ = new TaskLoopGroupMock(threadFactory_, 3);
        TaskSelection ts1 = mock(TaskSelection.class);
        TaskSelection ts2 = mock(TaskSelection.class);
        TaskSelection ts3 = mock(TaskSelection.class);
        TaskSelection ts = mock(TaskSelection.class);

        TaskLoopMock t1 = sut_.assign(ts1);
        TaskLoopMock t2 = sut_.assign(ts2);
        TaskLoopMock t3 = sut_.assign(ts3);
        TaskLoopMock t = sut_.assign(ts);

        assertThat(t1.selectionCount(), is(2)); // 1 + 3
        assertThat(t2.selectionCount(), is(1));
        assertThat(t3.selectionCount(), is(1));
        assertThat(t, is(sameInstance(t1)));
        assertThat(t, is(not(sameInstance(t2))));
        assertThat(t, is(not(sameInstance(t3))));
    }

    @Test(expected = NullPointerException.class)
    public void testAssign_ExceptionIfSelectionIsNull() throws Exception {
        sut_ = new TaskLoopGroupMock(threadFactory_, 1);
        sut_.assign(null);
    }

    @Test(timeout = 1000)
    public void testAssign_SweepDeadLoop() throws Exception {
        sut_ = new TaskLoopGroupMock(threadFactory_, 3);
        TaskSelection ts1 = mock(TaskSelection.class);
        TaskSelection ts2 = mock(TaskSelection.class);
        TaskSelection ts3 = mock(TaskSelection.class);

        TaskLoopMock t1 = sut_.assign(ts1);
        TaskLoopMock t2 = sut_.assign(ts2);
        TaskLoopMock t3 = sut_.assign(ts3);
        t2.close();
        while (t2.selectionCount() > 0) {
            Thread.sleep(10);
        }
        TaskLoopMock t = sut_.assign(ts2);

        assertThat(t, is(not(sameInstance(t1))));
        assertThat(t, is(not(sameInstance(t2))));
        assertThat(t, is(not(sameInstance(t3))));
    }

    @Test
    public void testAssign_ReturnsTheSameTaskLoopForTheSameSelection() throws Exception {
        sut_ = new TaskLoopGroupMock(threadFactory_, 3);
        TaskSelection ts1 = mock(TaskSelection.class);

        TaskLoopMock t0 = sut_.assign(ts1);
        TaskLoopMock t1 = sut_.assign(ts1);

        assertThat(t0, is(sameInstance(t1)));
        assertThat(t0.selectionCount(), is(1));
    }

    private static class TaskLoopGroupMock extends TaskLoopGroup<TaskLoopMock> {

        /**
         * Constructs a new instance.
         *
         * @param threadFactory a factory to create thread which runs a task loop
         * @param workers       the number of threads held in the thread pool
         */
        protected TaskLoopGroupMock(ThreadFactory threadFactory, int workers) {
            super(threadFactory, workers);
        }

        @Override
        protected TaskLoopMock newTaskLoop() {
            return new TaskLoopMock();
        }
    }
}
