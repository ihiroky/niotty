package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.Event;
import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.Transport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class StoreShutterTest {

    private StoreShutter sut_;
    private StageContext context_;
    private Transport transport_;

    @Before
    public void setUp() throws Exception {
        context_ = mock(StageContext.class);
        sut_ = new StoreShutter(10, 20, 1);
        transport_ = mock(Transport.class);

        when(context_.transport()).thenReturn(transport_);
    }
    
    private long runCheck(StageContext context) throws Exception {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(context).schedule(eventCaptor.capture(), anyLong(), Mockito.any(TimeUnit.class));
        Event event = eventCaptor.getValue();
        return event.execute(TimeUnit.NANOSECONDS);
    }

    @Test
    public void testUnderAlertToUnderAlert() throws Exception {
        when(transport_.pendingWriteBuffers()).thenReturn(10);
        sut_.activated(context_);

        long interval = runCheck(context_);

        assertThat(sut_.state(), is(StoreShutter.State.UNDER_ALERT));
        assertThat(interval, is(TimeUnit.SECONDS.toNanos(1L)));
    }

    @Test
    public void testUnderAlertToOverAlert() throws Exception {
        when(transport_.pendingWriteBuffers()).thenReturn(11);
        sut_.activated(context_);

        long interval = runCheck(context_);

        assertThat(sut_.state(), is(StoreShutter.State.OVER_ALERT));
        assertThat(interval, is(TimeUnit.SECONDS.toNanos(1L)));
    }

    @Test
    public void testUnderAlertToOverLimit() throws Exception {
        when(transport_.pendingWriteBuffers()).thenReturn(21);
        sut_.activated(context_);

        long interval = runCheck(context_);

        assertThat(sut_.state(), is(StoreShutter.State.OVER_LIMIT));
        assertThat(interval, is(Event.DONE));
    }

    @Test
    public void testOverAlertToUnderAlert() throws Exception {
        when(transport_.pendingWriteBuffers()).thenReturn(10);
        sut_.activated(context_);
        sut_.setState(StoreShutter.State.OVER_ALERT);

        long interval = runCheck(context_);

        assertThat(sut_.state(), is(StoreShutter.State.UNDER_ALERT));
        assertThat(interval, is(TimeUnit.SECONDS.toNanos(1L)));
    }

    @Test
    public void testOverAlertToOverAlert() throws Exception {
        when(transport_.pendingWriteBuffers()).thenReturn(20);
        sut_.activated(context_);
        sut_.setState(StoreShutter.State.OVER_ALERT);

        long interval = runCheck(context_);

        assertThat(sut_.state(), is(StoreShutter.State.OVER_ALERT));
        assertThat(interval, is(TimeUnit.SECONDS.toNanos(1L)));
    }

    @Test
    public void testOverAlertToOverLimit() throws Exception {
        when(transport_.pendingWriteBuffers()).thenReturn(21);
        sut_.activated(context_);
        sut_.setState(StoreShutter.State.OVER_ALERT);

        long interval = runCheck(context_);

        assertThat(sut_.state(), is(StoreShutter.State.OVER_LIMIT));
        assertThat(interval, is(Event.DONE));
    }

    @Test
    public void testDeactivated() throws Exception {
        sut_.deactivated(context_);

        assertThat(sut_.state(), is(StoreShutter.State.OVER_LIMIT));
    }

    @Test
    public void testStoreUnderAlert() throws Exception {
        Object m = new Object();
        Object p = new Object();

        sut_.stored(context_, m, p);

        verify(context_).proceed(m, p);
    }

    @Test
    public void testStoreOverAlert() throws Exception {
        Object m = new Object();
        Object p = new Object();
        sut_.setState(StoreShutter.State.OVER_ALERT);

        sut_.stored(context_, m, p);

        verify(context_).proceed(m, p);
    }

    @Test
    public void testStoreOverLimit() throws Exception {
        Object m = new Object();
        Object p = new Object();
        sut_.setState(StoreShutter.State.OVER_LIMIT);

        sut_.stored(context_, m, p);

        verify(context_, never()).proceed(m, p);
    }
}
