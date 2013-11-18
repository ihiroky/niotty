package net.ihiroky.niotty.codec;

import net.ihiroky.niotty.StageContext;
import net.ihiroky.niotty.Task;
import net.ihiroky.niotty.TaskFuture;
import net.ihiroky.niotty.buffer.Buffers;
import net.ihiroky.niotty.buffer.CodecBuffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DeficitRoundRobinEncoderTest {

    private StageContext context_;
    private Task timer_;

    @Before
    public void setUp() throws Exception {
        context_ = mock(StageContext.class);
        doAnswer(new Answer<TaskFuture>() {
            @Override
            public TaskFuture answer(InvocationOnMock invocation) throws Throwable {
                timer_ = (Task) invocation.getArguments()[0];
                return new TaskFuture(0, timer_);
            }
        }).when(context_).schedule(Mockito.<Task>any(), anyLong(), Mockito.<TimeUnit>any());
    }

    @Test
    public void testProceedNormalPriority() throws Exception {
        DeficitRoundRobinEncoder sut = new DeficitRoundRobinEncoder(0.5f);
        CodecBuffer b0 = Buffers.wrap(new byte[10]);
        CodecBuffer b1 = Buffers.wrap(new byte[10]);

        sut.stored(context_, b0);
        sut.stored(context_, b1);

        ArgumentCaptor<CodecBuffer> messageCaptor = ArgumentCaptor.forClass(CodecBuffer.class);
        verify(context_, times(2)).proceed(messageCaptor.capture());
        List<CodecBuffer> messageList = messageCaptor.getAllValues();
        assertThat(messageList.get(0), is(b0));
        assertThat(messageList.get(1), is(b1));
    }

    @Test
    public void testSmoothedBaseQuantum() throws Exception {
        DeficitRoundRobinEncoder sut = new DeficitRoundRobinEncoder(256, 1, TimeUnit.MILLISECONDS, 1f);
        WeightedMessage wm0 = new WeightedMessage(Buffers.wrap(new byte[64]));

        sut.stored(context_, wm0);
        int v0 = sut.smoothedBaseQuantum();
        sut.stored(context_, wm0);
        int v1 = sut.smoothedBaseQuantum();

        assertThat(v0, is(256 * 7 / 8 + 64 / 8));
        assertThat(v1, is(v0 * 7 / 8 + 64 / 8));
    }

    @Test
    public void testRoundOne() throws Exception {
        DeficitRoundRobinEncoder sut = new DeficitRoundRobinEncoder(64, 1L, TimeUnit.MILLISECONDS, 0.5f, 0.25f);
        CodecBuffer b  = Buffers.wrap(new byte[64]);
        CodecBuffer b0 = Buffers.wrap(new byte[64]);
        CodecBuffer b1 = Buffers.wrap(new byte[64]);

        sut.stored(context_, new WeightedMessage(b, -1));
        sut.stored(context_, new WeightedMessage(b0, 0));
        sut.stored(context_, new WeightedMessage(b1, 1));

        ArgumentCaptor<Long> timerDelayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(context_).proceed(b);
        verify(context_).schedule(Mockito.<Task>any(), timerDelayCaptor.capture(), Mockito.<TimeUnit>any());
        assertThat(timerDelayCaptor.getValue(), is(TimeUnit.MILLISECONDS.toNanos(1L)));
        assertThat(sut.deficitCounter(0), is(64 / 2));
        assertThat(sut.deficitCounter(1), is(64 / 4));
        assertThat(sut.queue(0).size(), is(1));
        assertThat(sut.queue(1).size(), is(1));
    }

    @Test
    public void testRoundTwo() throws Exception {
        DeficitRoundRobinEncoder sut = new DeficitRoundRobinEncoder(64, 1L, TimeUnit.MILLISECONDS, 0.5f, 0.25f);
        CodecBuffer b  = Buffers.wrap(new byte[64]);
        CodecBuffer b0 = Buffers.wrap(new byte[64]);
        CodecBuffer b1 = Buffers.wrap(new byte[64]);

        for (int i = 0; i < 2; i++) {
            sut.stored(context_, new WeightedMessage(b, -1));
            sut.stored(context_, new WeightedMessage(b0, 0));
            sut.stored(context_, new WeightedMessage(b1, 1));
        }

        ArgumentCaptor<CodecBuffer> proceededCaptor = ArgumentCaptor.forClass(CodecBuffer.class);
        ArgumentCaptor<Long> timerDelayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(context_, times(3)).proceed(proceededCaptor.capture());
        verify(context_).schedule(Mockito.<Task>any(), timerDelayCaptor.capture(), Mockito.<TimeUnit>any());
        List<CodecBuffer> proceededList = proceededCaptor.getAllValues();
        assertThat(proceededList.get(0), is(sameInstance(b)));
        assertThat(proceededList.get(1), is(sameInstance(b)));
        assertThat(proceededList.get(2), is(sameInstance(b0)));
        assertThat(timerDelayCaptor.getValue(), is(TimeUnit.MILLISECONDS.toNanos(1L)));
        assertThat(sut.deficitCounter(0), is(0));
        assertThat(sut.deficitCounter(1), is(64 / 4 * 2));
        assertThat(sut.queue(0).size(), is(1));
        assertThat(sut.queue(1).size(), is(2));
    }

    @Test
    public void testTimerAndThenFlushing() throws Exception {
        DeficitRoundRobinEncoder sut = new DeficitRoundRobinEncoder(64, 1L, TimeUnit.MILLISECONDS, 0.5f, 0.25f);
        CodecBuffer b0 = Buffers.wrap(new byte[64]);

        sut.stored(context_, new WeightedMessage(b0, 0));
        long delay0 = timer_.execute(TimeUnit.NANOSECONDS);

        assertThat(delay0, is(TimeUnit.MILLISECONDS.toNanos(1L)));
        assertThat(sut.deficitCounter(0), is(64 / 2));
        assertThat(sut.queue(0).size(), is(1));
        assertThat(sut.timerFuture(), is(notNullValue()));
    }

    @Test
    public void testTimerAndThenFlushed() throws Exception {
        DeficitRoundRobinEncoder sut = new DeficitRoundRobinEncoder(64, 1L, TimeUnit.MILLISECONDS, 0.5f, 0.25f);
        CodecBuffer b0 = Buffers.wrap(new byte[64]);

        sut.stored(context_, new WeightedMessage(b0, 0));
        long delay0 = timer_.execute(TimeUnit.NANOSECONDS);
        long delay1 = timer_.execute(TimeUnit.NANOSECONDS);

        assertThat(delay0, is(TimeUnit.MILLISECONDS.toNanos(1L)));
        assertThat(delay1, is(Task.DONE));
        assertThat(sut.deficitCounter(0), is(0));
        assertThat(sut.queue(0).size(), is(0));
        assertThat(sut.timerFuture(), is(nullValue()));
    }

    @Test
    public void testFlushFirstIfSmoothedBaseQuantumIsZeroByTimer() throws Exception {
        DeficitRoundRobinEncoder sut = new DeficitRoundRobinEncoder(1, 1L, TimeUnit.MILLISECONDS, 0.5f, 0.25f);
        CodecBuffer b = Buffers.wrap(new byte[0]);
        CodecBuffer b0 = Buffers.wrap(new byte[64]);
        CodecBuffer b1 = Buffers.wrap(new byte[64]);

        sut.stored(context_, new WeightedMessage(b, -1));
        int smoothedBaseQuantum = sut.smoothedBaseQuantum();
        sut.stored(context_, new WeightedMessage(b0, 0));
        sut.stored(context_, new WeightedMessage(b1, 1));
        long delay0 = timer_.execute(TimeUnit.NANOSECONDS);

        assertThat(smoothedBaseQuantum, is(0));
        assertThat(delay0, is(TimeUnit.MILLISECONDS.toNanos(1L)));
        assertThat(sut.deficitCounter(0), is(0));
        assertThat(sut.deficitCounter(1), is(64 * 2 / 4));
        assertThat(sut.queue(0).size(), is(0));
        assertThat(sut.queue(1).size(), is(1));
        assertThat(sut.timerFuture(), is(notNullValue()));
    }
}
