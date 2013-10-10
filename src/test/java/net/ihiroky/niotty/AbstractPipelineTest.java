package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class AbstractPipelineTest {

    private AbstractPipeline<Object, TaskLoop> sut_;
    private AbstractTransport<TaskLoop> transport_;
    private TaskLoopGroup<TaskLoop> taskLoopGroup_;
    private TaskLoop taskLoop_;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        transport_ = mock(AbstractTransport.class);
        taskLoopGroup_ = mock(TaskLoopGroup.class);
        taskLoop_ = mock(TaskLoop.class);
        when(taskLoopGroup_.assign(transport_)).thenReturn(taskLoop_);
        sut_ = new PipelineImpl(transport_, taskLoopGroup_);
    }

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testAdd() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testAdd_NullTaskLoopGroupReplacesToDefault() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();

        sut_.add(key0, stage0, null);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> pe = (PipelineElement<Object, Object>) sut_.searchContext(key0);
        assertThat(pe.taskLoop(), is(sameInstance(taskLoop_)));
    }

    @Test
    public void testAdd_KeyAlreadyExists() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("key IntStageKey:1 already exists.");

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key1, stage1);
    }

    @Test
    public void testAdd_IOStageIsRejected() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be added.");

        sut_.add(StageKeys.of("IO_STAGE_KEY"), new Object());
    }

    @Test
    public void testAddBefore() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut_.add(key0, stage0);
        sut_.addBefore(key0, key1, stage1);
        sut_.addBefore(key0, key2, stage2);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testAddBefore_NullTaskLoopGroupReplacesToDefault() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();

        sut_.add(key0, stage0);
        sut_.addBefore(key0, key1, stage1, null);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> pe = (PipelineElement<Object, Object>) sut_.searchContext(key1);
        assertThat(pe.taskLoop(), is(sameInstance(taskLoop_)));
    }

    @Test
    public void testAddBefore_KeyAlreadyExists() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("key IntStageKey:0 already exists.");

        sut_.add(key0, stage0);
        sut_.addBefore(key0, key1, stage1);
        sut_.addBefore(key0, key2, stage2);
        sut_.addBefore(key0, key0, stage0);
    }

    @Test
    public void testAddBefore_NoBaseKeyFound() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        exceptionRule_.expect(NoSuchElementException.class);
        exceptionRule_.expectMessage("baseKey IntStageKey:3 is not found.");

        sut_.add(key0, stage0);
        sut_.addBefore(key0, key1, stage1);
        sut_.addBefore(key3, key2, stage2);
    }

    @Test
    public void testAddBefore_IOStageIsRejected() throws Exception {
        StageKey key = StageKeys.of(0);
        sut_.add(key, new Object());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be added.");

        sut_.addBefore(key, StageKeys.of("IO_STAGE_KEY"), new Object());
    }

    @Test
    public void testAddAfter() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut_.add(key0, stage0);
        sut_.addAfter(key0, key1, stage1);
        sut_.addAfter(key0, key2, stage2);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testAddAfter_NullTaskLoopGroupReplacesToDefault() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();

        sut_.add(key0, stage0);
        sut_.addAfter(key0, key1, stage1, null);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> pe = (PipelineElement<Object, Object>) sut_.searchContext(key1);
        assertThat(pe.taskLoop(), is(sameInstance(taskLoop_)));
    }

    @Test
    public void testAddAfter_KeyAlreadyExists() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("key IntStageKey:0 already exists.");

        sut_.add(key0, stage0);
        sut_.addAfter(key0, key1, stage1);
        sut_.addAfter(key0, key2, stage2);
        sut_.addAfter(key0, key0, stage0);
    }

    @Test
    public void testAddAfter_NoBaseKeyFound() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        exceptionRule_.expect(NoSuchElementException.class);
        exceptionRule_.expectMessage("baseKey IntStageKey:3 is not found.");

        sut_.add(key0, stage0);
        sut_.addAfter(key0, key1, stage1);
        sut_.addAfter(key3, key2, stage2);
    }

    @Test
    public void testAddAfter_IOStageIsRejected() throws Exception {
        StageKey key = StageKeys.of(0);
        sut_.add(key, new Object());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be added.");

        sut_.addAfter(key, StageKeys.of("IO_STAGE_KEY"), new Object());
    }

    @Test
    public void testAddAfter_BaseKeyMustNotBeIOStage() throws Exception {
        StageKey key = StageKeys.of(0);
        sut_.add(key, new Object());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must be the tail of this pipeline.");

        sut_.addAfter(StageKeys.of("IO_STAGE_KEY"), key, new Object());
    }

    @Test
    public void testReplace_NullTaskLoopGroupReplacesToDefault() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();

        sut_.add(key0, stage0);
        sut_.replace(key0, key1, stage1, null);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> pe = (PipelineElement<Object, Object>) sut_.searchContext(key1);
        assertThat(pe.taskLoop(), is(sameInstance(taskLoop_)));
    }

    @Test
    public void testRemove_First() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.remove(key0);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testRemove_Middle() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.remove(key1);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testRemove_Last() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.remove(key2);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testRemove_NoKeyIsFound() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        exceptionRule_.expect(NoSuchElementException.class);
        exceptionRule_.expectMessage("key IntStageKey:2 is not found.");

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.remove(key2);
    }

    @Test
    public void testRemove_IOStageIsRejected() throws Exception {
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be removed.");

        sut_.remove(StageKeys.of("IO_STAGE_KEY"));
    }

    @Test
    public void testReplace_First() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        Object stage3 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key0, key3, stage3);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key3));
        assertThat(context.stage(), is(stage3));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testReplace_Middle() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        Object stage3 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key1, key3, stage3);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key3));
        assertThat(context.stage(), is(stage3));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testReplace_Last() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        Object stage3 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key2, key3, stage3);

        Iterator<PipelineElement<Object, Object>> i = sut_.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key3));
        assertThat(context.stage(), is(stage3));
        assertThat(i.hasNext(), is(true));
        context = i.next();
        assertThat(context.key(), is(PipelineImpl.LAST));
    }

    @Test
    public void testReplace_OldKeyIsNotFound() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        Object stage3 = new Object();
        exceptionRule_.expect(NoSuchElementException.class);
        exceptionRule_.expectMessage("oldKey IntStageKey:3 is not found.");

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key3, key3, stage3);
    }

    @Test
    public void testReplace_NewKeyAlreadyExists() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("newKey IntStageKey:2 already exists.");

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key3, key2, stage2);
    }

    @Test
    public void testReplace_NewIOStageIsRejected() throws Exception {
        StageKey key = StageKeys.of(0);
        sut_.add(key, new Object());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be added.");

        sut_.replace(key, StageKeys.of("IO_STAGE_KEY"), new Object());
    }

    @Test
    public void testReplace_OldIOStageIsRejected() throws Exception {
        StageKey key = StageKeys.of(0);
        sut_.add(key, new Object());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be removed.");

        sut_.replace(StageKeys.of("IO_STAGE_KEY"), key, new Object());
    }

    @Test
    public void testSearchContext() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);

        StageContext<Object> context0 = sut_.searchContext(key0);
        StageContext<Object> context1 = sut_.searchContext(key1);
        StageContext<Object> context2 = sut_.searchContext(PipelineImpl.LAST);
        assertThat(context0.key(), is(key0));
        assertThat(context1.key(), is(key1));
        assertThat(context2.key(), is(PipelineImpl.LAST));

    }

    @Test
    public void testExecuteMessage_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        sut_.add(StageKeys.of(0), new Object());
        Object message = new Object();

        sut_.execute(message);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> context = (PipelineElement<Object, Object>) sut_.searchContext(StageKeys.of(0));
        verify(context).fire(message);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testExecuteMessage_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        sut_.add(StageKeys.of(0), new Object());
        Object message = new Object();

        sut_.execute(message);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> context = (PipelineElement<Object, Object>) sut_.searchContext(StageKeys.of(0));
        verify(context, never()).fire(message);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(context).fire(message);
    }

    @Test
    public void testExecuteMessageAndParameter_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        sut_.add(StageKeys.of(0), new Object());
        Object message = new Object();
        TransportParameter parameter = new DefaultTransportParameter(0);

        sut_.execute(message, parameter);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> context = (PipelineElement<Object, Object>) sut_.searchContext(StageKeys.of(0));
        verify(context).fire(message, parameter);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testExecuteMessageAndParameter_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        sut_.add(StageKeys.of(0), new Object());
        Object message = new Object();
        TransportParameter parameter = new DefaultTransportParameter(0);

        sut_.execute(message, parameter);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> context = (PipelineElement<Object, Object>) sut_.searchContext(StageKeys.of(0));
        verify(context, never()).fire(message, parameter);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(context).fire(message, parameter);
    }

    @Test
    public void testExecuteTransportStateEvent_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        sut_.add(StageKeys.of(0), new Object());
        TransportStateEvent event = new DefaultTransportStateEvent(TransportState.BOUND, new Object());

        sut_.execute(event);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> context = (PipelineElement<Object, Object>) sut_.searchContext(StageKeys.of(0));
        verify(context).fire(event);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testExecuteTransportStateEvent_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        sut_.add(StageKeys.of(0), new Object());
        TransportStateEvent event = new DefaultTransportStateEvent(TransportState.BOUND, new Object());

        sut_.execute(event);

        @SuppressWarnings("unchecked")
        PipelineElement<Object, Object> context = (PipelineElement<Object, Object>) sut_.searchContext(StageKeys.of(0));
        verify(context, never()).fire(event);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(context).fire(event);
    }

    private static class PipelineImpl extends AbstractPipeline<Object, TaskLoop> {

        static final StageKey LAST = StageKeys.of("LAST");

        @SuppressWarnings("unchecked")
        protected PipelineImpl(AbstractTransport<TaskLoop> transport, TaskLoopGroup<TaskLoop> taskLoopGroup) {
            super("test", transport, taskLoopGroup, LAST, new Object());
        }

        @Override
        protected PipelineElement<Object, Object> createContext(
                StageKey key, final Object stage, TaskLoopGroup<? extends TaskLoop> pool) {
            PipelineElement<Object, Object> context = new PipelineElement<Object, Object>(this, key, pool) {
                @Override
                protected Object stage() {
                    return stage;
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
                    return DefaultTransportParameter.NO_PARAMETER;
                }
            };
            return spy(context);
        }
    }
}
