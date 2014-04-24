package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class AbstractTransportFutureTest {

    private DefaultTransportFuture sut_;
    private AbstractTransport<?> transport_;
    private EventDispatcher eventDispatcher_;

    @Before
    public void setUp() {
        @SuppressWarnings("unchecked")
        AbstractTransport<EventDispatcher> transport = mock(AbstractTransport.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        when(transport.eventDispatcher()).thenReturn(eventDispatcher);

        transport_ = transport;
        eventDispatcher_ = eventDispatcher;
        sut_ = new DefaultTransportFuture(transport);
    }

    @Test
    public void testOneListenerIsCalledOnCompleteIfNotDoneAtAdded() throws Exception {
        CompletionListener listener = mock(CompletionListener.class);
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(true);

        sut_.addListener(listener);
        sut_.fireOnComplete();

        verify(listener).onComplete(sut_);
    }

    @Test
    public void testListenersIsCalledOnCompleteIfNotDoneAtAdded() throws Exception {
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(true);
        CompletionListener listener0 = mock(CompletionListener.class);
        CompletionListener listener1 = mock(CompletionListener.class);
        CompletionListener listener2 = mock(CompletionListener.class);

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
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(true);
        CompletionListener listener = mock(CompletionListener.class);

        sut_.executing();
        sut_.done();
        sut_.addListener(listener);

        verify(listener).onComplete(sut_);
    }

    @Test
    public void testListenersIsCalledOnAddListenerIfDone() throws Exception {
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(true);
        CompletionListener listener0 = mock(CompletionListener.class);
        CompletionListener listener1 = mock(CompletionListener.class);
        CompletionListener listener2 = mock(CompletionListener.class);

        sut_.executing();
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
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(true);
        CompletionListener listener = mock(CompletionListener.class);

        sut_.addListener(listener);
        sut_.removeListener(listener);
        sut_.fireOnComplete();

        verify(listener, never()).onComplete(sut_);
    }

    @Test
    public void testRemoveListener_InternalListenerGetsNormalListener() throws Exception {
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(true);
        CompletionListener listener0 = mock(CompletionListener.class);
        CompletionListener listener1 = mock(CompletionListener.class);

        sut_.addListener(listener0);
        sut_.addListener(listener1);
        sut_.removeListener(listener0);
        sut_.fireOnComplete();

        verify(listener0, never()).onComplete(sut_);
        verify(listener1).onComplete(sut_);
    }

    @Test
    public void testRemoveListener_InternalListenerGetsListenerList() throws Exception {
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(true);
        CompletionListener listener0 = mock(CompletionListener.class);
        CompletionListener listener1 = mock(CompletionListener.class);
        CompletionListener listener2 = mock(CompletionListener.class);

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
    public void testFireOnComplete_CallsOfferEvent() throws Exception {
        final long[] eventResult = new long[]{0L};
        when(transport_.eventDispatcher().isInDispatcherThread()).thenReturn(false);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Event event = (Event) invocation.getArguments()[0];
                eventResult[0] = event.execute();
                return null;
            }
        }).when(eventDispatcher_).offer(Mockito.any(Event.class));
        CompletionListener listener = mock(CompletionListener.class);

        sut_.addListener(listener);
        sut_.fireOnComplete();

        verify(listener).onComplete(sut_);
        assertThat(eventResult[0], is(Event.DONE));
    }
}
