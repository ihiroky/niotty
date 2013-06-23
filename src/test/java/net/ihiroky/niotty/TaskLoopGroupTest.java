package net.ihiroky.niotty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
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
        sut_ = new TaskLoopGroupMock();
        threadFactory_ = new NameCountThreadFactory("TaskLoopGroupTest");
    }

    @After
    public void tearDown() {
        sut_.close();
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsOpen() throws Exception {
        sut_.open(threadFactory_, 1);

        assertThat(sut_.isOpen(), is(true));
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsNotOpen() throws Exception {
        assertThat(sut_.isOpen(), is(false));
    }

    @Test
    public void testIsOpen_ReturnsTrueIfSutIsReopen() throws Exception {
        sut_.open(threadFactory_, 1);
        sut_.close();
        sut_.open(threadFactory_, 1);

        assertThat(sut_.isOpen(), is(true));
    }

    @Test
    public void testAssign_ReturnsMinimumWeightTaskLoop() throws Exception {
        sut_.open(threadFactory_, 3);
        TaskSelection ts1 = mock(TaskSelection.class);
        when(ts1.weight()).thenReturn(1);
        TaskSelection ts2 = mock(TaskSelection.class);
        when(ts2.weight()).thenReturn(2);
        TaskSelection ts3 = mock(TaskSelection.class);
        when(ts3.weight()).thenReturn(3);
        TaskSelection ts = mock(TaskSelection.class);
        when(ts.weight()).thenReturn(3);

        TaskLoopMock t1 = sut_.assign(ts1);
        TaskLoopMock t2 = sut_.assign(ts2);
        TaskLoopMock t3 = sut_.assign(ts3);
        TaskLoopMock t = sut_.assign(ts);

        assertThat(t1.weight(), is(4)); // 1 + 3
        assertThat(t2.weight(), is(2));
        assertThat(t3.weight(), is(3));
        assertThat(t, is(sameInstance(t1)));
        assertThat(t, is(not(sameInstance(t2))));
        assertThat(t, is(not(sameInstance(t3))));
    }

    @Test
    public void testAssign_ReturnUnderThresholdTaskLoop() throws Exception {
        sut_.setTaskWeightThreshold(3);
        sut_.open(threadFactory_, 3);

        TaskSelection ts1 = mock(TaskSelection.class);
        when(ts1.weight()).thenReturn(1);
        TaskSelection ts2 = mock(TaskSelection.class);
        when(ts2.weight()).thenReturn(2);
        TaskSelection ts3 = mock(TaskSelection.class);
        when(ts3.weight()).thenReturn(3);

        TaskLoopMock t1 = sut_.assign(ts1);
        TaskLoopMock t2 = sut_.assign(ts2);
        TaskLoopMock t3 = sut_.assign(ts3);
        List<TaskLoopMock> taskLoopsView = sut_.taskLoopsView();
        Collections.sort(taskLoopsView);

        assertThat(t1, is(sameInstance(t2)));
        assertThat(t1, is(not(sameInstance(t3))));
        assertThat(taskLoopsView.get(0).weight(), is(0));
        assertThat(taskLoopsView.get(1).weight(), is(3));
        assertThat(taskLoopsView.get(2).weight(), is(3));
    }

    @Test
    public void testAssign_ReturnNewTaskLoopIfThereIsNoUnderThreshold() throws Exception {
        sut_.setTaskWeightThreshold(1);
        sut_.open(threadFactory_, 2, 3);

        TaskSelection ts1 = mock(TaskSelection.class);
        when(ts1.weight()).thenReturn(1);
        TaskSelection ts2 = mock(TaskSelection.class);
        when(ts2.weight()).thenReturn(2);
        TaskSelection ts3 = mock(TaskSelection.class);
        when(ts3.weight()).thenReturn(3);

        TaskLoopMock t1 = sut_.assign(ts1);
        TaskLoopMock t2 = sut_.assign(ts2);
        TaskLoopMock t3 = sut_.assign(ts3);

        assertThat(t1, is(not(sameInstance(t2))));
        assertThat(t1, is(not(sameInstance(t3))));
        assertThat(sut_.pooledTaskLoops(), is(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssign_ExceptionIfWeightOfSelectionIsZero() throws Exception {
        TaskSelection ts = mock(TaskSelection.class);
        when(ts.weight()).thenReturn(0);
        sut_.assign(ts);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssign_ExceptionIfWeightOfSelectionIsNegative() throws Exception {
        TaskSelection ts = mock(TaskSelection.class);
        when(ts.weight()).thenReturn(-1);
        sut_.assign(ts);
    }

    @Test(expected = IllegalStateException.class)
    public void testAssign_ExceptionIfSutIsNotOpen() throws Exception {
        TaskSelection ts = mock(TaskSelection.class);
        when(ts.weight()).thenReturn(1);
        sut_.assign(ts);
    }

    @Test(timeout = 1000)
    public void testAssign_SweepDeadLoop() throws Exception {
        sut_.open(threadFactory_, 3);
        TaskSelection ts1 = mock(TaskSelection.class);
        when(ts1.weight()).thenReturn(1);
        TaskSelection ts2 = mock(TaskSelection.class);
        when(ts2.weight()).thenReturn(1);
        TaskSelection ts3 = mock(TaskSelection.class);
        when(ts3.weight()).thenReturn(1);

        TaskLoopMock t1 = sut_.assign(ts1);
        TaskLoopMock t2 = sut_.assign(ts2);
        TaskLoopMock t3 = sut_.assign(ts3);
        t2.close();
        while (t2.selectionView().size() > 0) {
            Thread.sleep(10);
        }
        TaskLoopMock t = sut_.assign(ts2);

        assertThat(t, is(not(sameInstance(t1))));
        assertThat(t, is(not(sameInstance(t2))));
        assertThat(t, is(not(sameInstance(t3))));
    }

    @Test
    public void testAssign_ReturnsTheSameTaskLoopForTheSameSelection() throws Exception {
        sut_.open(threadFactory_, 3);
        TaskSelection ts1 = mock(TaskSelection.class);
        when(ts1.weight()).thenReturn(1);

        TaskLoopMock t0 = sut_.assign(ts1);
        TaskLoopMock t1 = sut_.assign(ts1);

        assertThat(t0, is(sameInstance(t1)));
    }

    private static class TaskLoopGroupMock extends TaskLoopGroup<TaskLoopMock> {

        @Override
        protected TaskLoopMock newTaskLoop() {
            return new TaskLoopMock();
        }
    }
}
