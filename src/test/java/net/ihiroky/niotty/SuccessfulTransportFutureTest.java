package net.ihiroky.niotty;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class SuccessfulTransportFutureTest {

    @Test
    public void testIsSuccessful_ReturnsTrueIfDoneAndNoThrowable() throws Exception {
        @SuppressWarnings("unchecked")
        AbstractTransport<?> transport = mock(AbstractTransport.class);
        FailedTransportFuture sut = new FailedTransportFuture(transport, new Exception());
        assertThat(sut.isSuccessful(), is(false));
    }

    @Test
    public void testThrowable_ReturnNullIfNotSetThrowable() throws Exception {
        @SuppressWarnings("unchecked")
        AbstractTransport<?> transport = mock(AbstractTransport.class);
        Exception e = new Exception();
        FailedTransportFuture sut = new FailedTransportFuture(transport, e);
        assertThat(sut.throwable(), is((Throwable) e));
    }
}
