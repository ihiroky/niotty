package net.ihiroky.niotty;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DefaultTaskTimerTest {

    private DefaultTaskTimer sut_;
    private TaskLoop taskLoop_;

    @Before
    public void setUp() throws Exception {
        taskLoop_ = mock(TaskLoop.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                TaskLoop.Task task = (TaskLoop.Task) invocation.getArguments()[0];
                task.execute(TimeUnit.MILLISECONDS);
                return null;
            }
        }).when(taskLoop_).offerTask(Mockito.any(TaskLoop.Task.class));
        sut_ = new DefaultTaskTimer("test");
        sut_.start();
    }

    @After
    public void tearDown() {
        sut_.stop();
        if (sut_.isAlive()) {
            throw new AssertionError();
        }
        taskLoop_.close();
    }

    @Test(timeout = 1000)
    public void testRun_PollTwiceSimultaneously() throws Exception {
        final boolean[] done = new boolean[2];
        TaskLoop.Task task0 = mock(TaskLoop.Task.class);
        when(task0.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[0] = true;
                return TaskLoop.DONE;
            }
        });
        TaskLoop.Task task1 = mock(TaskLoop.Task.class);
        when(task1.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                done[1] = true;
                return TaskLoop.DONE;
            }
        });

        TaskTimer.Future e0 = sut_.offer(taskLoop_, task0, 100, TimeUnit.MILLISECONDS);
        TaskTimer.Future e1 = sut_.offer(taskLoop_, task1, 100, TimeUnit.MILLISECONDS);

        while (!done[0] || !done[1]) {
            Thread.sleep(10);
        }
        verify(task0, timeout(120)).execute(TimeUnit.MILLISECONDS);
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        verify(task1, timeout(120)).execute(TimeUnit.MILLISECONDS);
        assertThat(e1.isDispatched(), is(true));
        assertThat(e1.isCancelled(), is(false));
    }

    @Test(timeout = 1000)
    public void testRun_Priority() throws Exception {
        final Queue<Integer> order = new ArrayBlockingQueue<>(10);
        TaskLoop.Task task0 = mock(TaskLoop.Task.class);
        when(task0.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(0);
                return TaskLoop.DONE;
            }
        });
        when(task0.toString()).thenReturn("task0");
        TaskLoop.Task task1 = mock(TaskLoop.Task.class);
        when(task1.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(1);
                return TaskLoop.DONE;
            }
        });
        when(task1.toString()).thenReturn("task1");

        TaskTimer.Future e0 = sut_.offer(taskLoop_, task0, 200, TimeUnit.MILLISECONDS);
        TaskTimer.Future e1 = sut_.offer(taskLoop_, task1, 100, TimeUnit.MILLISECONDS);

        while (order.size() < 2) {
            Thread.sleep(10);
        }
        assertThat(order.poll(), is(1));
        assertThat(order.poll(), is(0));
        verify(task0, timeout(220)).execute(TimeUnit.MILLISECONDS);
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        verify(task1, timeout(120)).execute(TimeUnit.MILLISECONDS);
        assertThat(e1.isDispatched(), is(true));
        assertThat(e1.isCancelled(), is(false));
    }

    @Test
    public void testIsAlive_ReturnFalseIfStopIsCalledAtTheSameTimesAsStart() throws Exception {
        DefaultTaskTimer sut = new DefaultTaskTimer("test");
        sut.start();
        sut.start();
        sut.start();
        sut.stop();
        assertThat(sut.isAlive(), is(true));
        sut.stop();
        assertThat(sut.isAlive(), is(true));
        sut.stop();
        assertThat(sut.isAlive(), is(false));
    }

    @Test
    public void testCancel_RemoveHeadEntryInTimerQueue() throws Exception {
        final Queue<Integer> order = new ArrayBlockingQueue<>(10);
        TaskLoop.Task task0 = mock(TaskLoop.Task.class);
        when(task0.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(0);
                return TaskLoop.DONE;
            }
        });

        TaskTimer.Future e = sut_.offer(taskLoop_, task0, 100, TimeUnit.MILLISECONDS);
        e.cancel();
        while (sut_.hasTask()) {
            Thread.sleep(10);
        }
        assertThat(order.isEmpty(), is(true));
        assertThat(e.isDispatched(), is(false));
        assertThat(e.isCancelled(), is(true));
    }

    @Test
    public void testCancel_RemoveHeadEntryInTimerQueueAfterExecute() throws Exception {
        final Queue<Integer> order = new ArrayBlockingQueue<>(10);
        TaskLoop.Task task0 = mock(TaskLoop.Task.class);
        when(task0.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(0);
                return TaskLoop.DONE;
            }
        });
        TaskLoop.Task task1 = mock(TaskLoop.Task.class);
        when(task1.execute(TimeUnit.MILLISECONDS)).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                order.offer(1);
                return TaskLoop.DONE;
            }
        });

        TaskTimer.Future e0 = sut_.offer(taskLoop_, task0, 100, TimeUnit.MILLISECONDS);
        TaskTimer.Future e1 = sut_.offer(taskLoop_, task1, 100, TimeUnit.MILLISECONDS);
        e1.cancel();
        while (sut_.hasTask()) {
            Thread.sleep(10);
        }
        assertThat(order.poll(), is(0));
        assertThat(order.isEmpty(), is(true));
        assertThat(e0.isDispatched(), is(true));
        assertThat(e0.isCancelled(), is(false));
        assertThat(e1.isDispatched(), is(false));
        assertThat(e1.isCancelled(), is(true));
    }

}
