package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class AbstractTransportFutureTest {

    private DefaultTransportFuture sut_;
    private AbstractTransport<?> transport_;
    private TaskLoop taskLoop_;

    @Before
    public void setUp() {
        @SuppressWarnings("unchecked")
        AbstractTransport<TaskLoop> transport = mock(AbstractTransport.class);
        TaskLoop taskLoop = mock(TaskLoop.class);
        when(transport.taskLoop()).thenReturn(taskLoop);

        transport_ = transport;
        taskLoop_ = taskLoop;
        sut_ = new DefaultTransportFuture(transport);
    }

    @Test
    public void testOneListenerIsCalledOnCompleteIfNotDoneAtAdded() throws Exception {
        TransportFutureListener listener = mock(TransportFutureListener.class);
        when(transport_.taskLoop().isInLoopThread()).thenReturn(true);

        sut_.addListener(listener);
        sut_.fireOnComplete();

        verify(listener).onComplete(sut_);
    }

    @Test
    public void testListenersIsCalledOnCompleteIfNotDoneAtAdded() throws Exception {
        when(transport_.taskLoop().isInLoopThread()).thenReturn(true);
        TransportFutureListener listener0 = mock(TransportFutureListener.class);
        TransportFutureListener listener1 = mock(TransportFutureListener.class);
        TransportFutureListener listener2 = mock(TransportFutureListener.class);

        sut_.addListener(listener0);
        sut_.addListener(listener1);
        sut_.addListener(listener2);
        sut_.fireOnComplete();

        verify(listener0).onComplete(sut_);
        verify(listener1).onComplete(sut_);
        verify(listener2).onComplete(sut_);
    }

    @Test
    public void testOneListenerIsCalledOnAddListenerIfDone() throws Exception {
        when(transport_.taskLoop().isInLoopThread()).thenReturn(true);
        TransportFutureListener listener = mock(TransportFutureListener.class);

        sut_.done();
        sut_.addListener(listener);

        verify(listener).onComplete(sut_);
    }

    @Test
    public void testListenersIsCalledOnAddListenerIfDone() throws Exception {
        when(transport_.taskLoop().isInLoopThread()).thenReturn(true);
        TransportFutureListener listener0 = mock(TransportFutureListener.class);
        TransportFutureListener listener1 = mock(TransportFutureListener.class);
        TransportFutureListener listener2 = mock(TransportFutureListener.class);

        sut_.done();
        sut_.addListener(listener0);
        sut_.addListener(listener1);
        sut_.addListener(listener2);

        verify(listener0).onComplete(sut_);
        verify(listener1).onComplete(sut_);
        verify(listener2).onComplete(sut_);
    }

    @Test
    public void testRemoveListener_InternalListenerGetsNullListener() throws Exception {
        when(transport_.taskLoop().isInLoopThread()).thenReturn(true);
        TransportFutureListener listener = mock(TransportFutureListener.class);

        sut_.addListener(listener);
        sut_.removeListener(listener);
        sut_.fireOnComplete();

        verify(listener, never()).onComplete(sut_);
    }

    @Test
    public void testRemoveListener_InternalListenerGetsNormalListener() throws Exception {
        when(transport_.taskLoop().isInLoopThread()).thenReturn(true);
        TransportFutureListener listener0 = mock(TransportFutureListener.class);
        TransportFutureListener listener1 = mock(TransportFutureListener.class);

        sut_.addListener(listener0);
        sut_.addListener(listener1);
        sut_.removeListener(listener0);
        sut_.fireOnComplete();

        verify(listener0, never()).onComplete(sut_);
        verify(listener1).onComplete(sut_);
    }

    @Test
    public void testRemoveListener_InternalListenerGetsListenerList() throws Exception {
        when(transport_.taskLoop().isInLoopThread()).thenReturn(true);
        TransportFutureListener listener0 = mock(TransportFutureListener.class);
        TransportFutureListener listener1 = mock(TransportFutureListener.class);
        TransportFutureListener listener2 = mock(TransportFutureListener.class);

        sut_.addListener(listener0);
        sut_.addListener(listener1);
        sut_.addListener(listener2);
        sut_.removeListener(listener0);
        sut_.fireOnComplete();

        verify(listener0, never()).onComplete(sut_);
        verify(listener1).onComplete(sut_);
        verify(listener2).onComplete(sut_);
    }

    @Test
    public void testFireOnComplete_CallsOfferTask() throws Exception {
        final long[] taskResult = new long[]{0L};
        when(transport_.taskLoop().isInLoopThread()).thenReturn(false);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Task task = (Task) invocation.getArguments()[0];
                taskResult[0] = task.execute(TimeUnit.MILLISECONDS);
                return null;
            }
        }).when(taskLoop_).offer(Mockito.any(Task.class));
        TransportFutureListener listener = mock(TransportFutureListener.class);

        sut_.addListener(listener);
        sut_.fireOnComplete();

        verify(listener).onComplete(sut_);
        assertThat(taskResult[0], is(Long.MAX_VALUE));
    }
}
