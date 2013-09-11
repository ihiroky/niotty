package net.ihiroky.niotty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DefaultTaskLoopGroupTest {

    private DefaultTaskLoopGroup sut_;

    @Before
    public void setUp() throws Exception {
        sut_ = new DefaultTaskLoopGroup(2);
    }

    @After
    public void tearDown() throws Exception {
        sut_.close();
    }

    @Test
    public void testAssign_TaskLoopSelectionCount() throws Exception {
        TaskSelection ts0 = mock(TaskSelection.class);
        TaskSelection ts1 = mock(TaskSelection.class);

        DefaultTaskLoop t0 = sut_.assign(ts0);
        DefaultTaskLoop t1 = sut_.assign(ts0);
        DefaultTaskLoop t2 = sut_.assign(ts1);

        assertThat(t0, is(sameInstance(t1)));
        assertThat(t0.selectionCount(), is(1));
        assertThat(t0.duplicationCountFor(ts0), is(2));
        assertThat(t2.selectionCount(), is(1));
        assertThat(t2.duplicationCountFor(ts1), is(1));
    }

    @Test
    public void testAssign_SelectionCountGetsZeroIfTaskLoopIsRejected() throws Exception {
        TaskSelection ts0 = mock(TaskSelection.class);
        TaskSelection ts1 = mock(TaskSelection.class);

        DefaultTaskLoop t0 = sut_.assign(ts0);
        DefaultTaskLoop t1 = sut_.assign(ts0);
        DefaultTaskLoop t2 = sut_.assign(ts1);
        t0.reject(ts0);
        int dupCount0 = t0.duplicationCountFor(ts0);
        t1.reject(ts0);
        int dupCount1 = t0.duplicationCountFor(ts0);
        t2.reject(ts1);

        assertThat(t0.selectionCount(), is(0));
        assertThat(t1.selectionCount(), is(0));
        assertThat(t2.selectionCount(), is(0));
        assertThat(dupCount0, is(1));
        assertThat(dupCount1, is(0));
    }

}
