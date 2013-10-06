package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 *
 */
public class PipelineElementTest {

    private PipelineElement<Object, Object> sut_;
    private TaskLoop taskLoop_;
    private PipelineElement<Object, Object> next_;
    private TaskLoop nextTaskLoop_;

    @Before
    public void setUp() throws Exception {
        taskLoop_ = mock(TaskLoop.class);
        sut_ = new PipelineElementImpl(taskLoop_);
        nextTaskLoop_ = mock(TaskLoop.class);
        next_ = spy(new PipelineElementImpl(nextTaskLoop_));
        sut_.setNext(next_);
    }

    @Test
    public void testProceedMessage_DirectlyIfInLoopThread() throws Exception {
        when(nextTaskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();

        sut_.proceed(message);

        verify(next_).fire(message);
        verify(nextTaskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testProceedMessage_IndirectlyIfNotInLoopThread() throws Exception {
        when(nextTaskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();

        sut_.proceed(message);

        verify(next_, never()).fire(message);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(nextTaskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(next_).fire(message);
    }

    @Test
    public void testProceedMessageAndParameter_DirectlyIfInLoopThread() throws Exception {
        when(nextTaskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();
        TransportParameter parameter = new DefaultTransportParameter(0);

        sut_.proceed(message, parameter);

        verify(next_).fire(message, parameter);
        verify(nextTaskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testProceedMessageAndParameter_IndirectlyIfNotInLoopThread() throws Exception {
        when(nextTaskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();
        TransportParameter parameter = new DefaultTransportParameter(0);

        sut_.proceed(message, parameter);

        verify(next_, never()).fire(message, parameter);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(nextTaskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(next_).fire(message, parameter);
    }

    @Test
    public void testProceedTransportStateEvent_DirectlyIfInLoopThread() throws Exception {
        TransportStateEvent event = new DefaultTransportStateEvent(TransportState.BOUND, new Object());
        when(nextTaskLoop_.isInLoopThread()).thenReturn(true);
        doNothing().when(next_).proceed(eq(event));

        sut_.proceed(event);

        verify(next_).fire(event);
        verify(next_).proceed(event);
        verify(nextTaskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testProceedTransportStateEvent_IndirectlyIfNotInLoopThread() throws Exception {
        TransportStateEvent event = new DefaultTransportStateEvent(TransportState.BOUND, new Object());
        when(nextTaskLoop_.isInLoopThread()).thenReturn(false);
        doNothing().when(next_).proceed(eq(event));

        sut_.proceed(event);

        verify(next_, never()).fire(event);
        verify(next_, never()).proceed(event);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(nextTaskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(next_).fire(event);
        verify(next_).proceed(event);
    }

    private static class PipelineElementImpl extends PipelineElement<Object, Object> {

        PipelineElementImpl(TaskLoop taskLoop) {
            super(taskLoop);
        }

        @Override
        protected Object stage() {
            return null;
        }

        @Override
        protected void fire(Object input) {
        }

        @Override
        protected void fire(Object input, TransportParameter parameter) {
        }

        @Override
        protected void fire(TransportStateEvent event) {
        }

        @Override
        public TransportParameter transportParameter() {
            return null;
        }
    }
}
