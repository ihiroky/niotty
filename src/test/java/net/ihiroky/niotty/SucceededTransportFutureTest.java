package net.ihiroky.niotty;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class SucceededTransportFutureTest {

    @Test
    public void testIsSuccessful_ReturnsTrueIfDoneAndNoThrowable() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(mock(Transport.class), new Exception());
        assertThat(sut.isSuccessful(), is(false));
    }

    @Test
    public void testThrowable_ReturnNullIfNotSetThrowable() throws Exception {
        Exception e = new Exception();
        FailedTransportFuture sut = new FailedTransportFuture(mock(Transport.class), e);
        assertThat(sut.throwable(), is((Throwable) e));
    }
}
