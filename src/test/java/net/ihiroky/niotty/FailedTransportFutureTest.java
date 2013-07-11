package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 * @author Hiroki Itoh
 */
public class FailedTransportFutureTest {

    private AbstractTransport<?> transport_;

    @Before
    public void setUp() {
        @SuppressWarnings("unchecked")
        AbstractTransport<?> transport = mock(AbstractTransport.class);
        transport_ = transport;
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowRuntimeExceptionIfFailed_ThrowableIsIndexOutOfBoundException() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new IndexOutOfBoundsException());
        sut.throwRuntimeExceptionIfFailed();
    }

    @Test
    public void testThrowRuntimeExceptionIfFailed_ThrowableIsIOException() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new IOException());

        try {
            sut.throwRuntimeExceptionIfFailed();
        } catch (RuntimeException re) {
            Throwable t = re.getCause();
            assertThat(t, is(instanceOf(IOException.class)));
        }
    }

    @Test(expected = AssertionError.class)
    public void testThrowRuntimeExceptionIfFailed_ThrowableIsAssertionError() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new AssertionError());
        sut.throwRuntimeExceptionIfFailed();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowExceptionIfFailed_ThrowableIsIndexOutOfBoundException() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new IndexOutOfBoundsException());
        sut.throwExceptionIfFailed();
    }

    @Test(expected = IOException.class)
    public void testThrowExceptionIfFailed_ThrowableIsIOException() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new IOException());
        sut.throwExceptionIfFailed();
    }

    @Test(expected = AssertionError.class)
    public void testThrowExceptionIfFailed_ThrowableIsAssertionError() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new AssertionError());
        sut.throwExceptionIfFailed();
    }

    @Test
    public void testIsCancelled() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new Exception());
        assertThat(sut.isCancelled(), is(false));
    }

    @Test
    public void testIsSuccessful_ReturnsTrueIfDoneAndNoThrowable() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new Exception());
        assertThat(sut.isSuccessful(), is(false));
    }

    @Test
    public void testIsDone_ReturnFalseByDefault() throws Exception {
        FailedTransportFuture sut = new FailedTransportFuture(transport_, new Exception());
        assertThat(sut.isDone(), is(true));
    }

    @Test
    public void testThrowable_ReturnNullIfNotSetThrowable() throws Exception {
        Exception e = new Exception();
        FailedTransportFuture sut = new FailedTransportFuture(transport_, e);
        assertThat(sut.throwable(), is((Throwable) e));
    }
}
