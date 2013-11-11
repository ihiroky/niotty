package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DefaultPipelineTest {

    private DefaultPipeline<TaskLoop> sut_;
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();

        sut_.add(key0, stage0, null);

        PipelineElement pe = (PipelineElement) sut_.searchElement(key0);
        assertThat(pe.taskLoop_, is(sameInstance(taskLoop_)));
    }

    @Test
    public void testAdd_KeyAlreadyExists() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
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

        sut_.add(StageKeys.of("IO_STAGE_KEY"), PipelineElement.newNullStage());
    }

    @Test
    public void testAddBefore() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.addBefore(key0, key1, stage1);
        sut_.addBefore(key0, key2, stage2);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.addBefore(key0, key1, stage1, null);

        PipelineElement pe = (PipelineElement) sut_.searchElement(key1);
        assertThat(pe.taskLoop_, is(sameInstance(taskLoop_)));
    }

    @Test
    public void testAddBefore_KeyAlreadyExists() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
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
        sut_.add(key, PipelineElement.newNullStage());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be added.");

        sut_.addBefore(key, StageKeys.of("IO_STAGE_KEY"), PipelineElement.newNullStage());
    }

    @Test
    public void testAddAfter() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.addAfter(key0, key1, stage1);
        sut_.addAfter(key0, key2, stage2);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.addAfter(key0, key1, stage1, null);

        PipelineElement pe = (PipelineElement) sut_.searchElement(key1);
        assertThat(pe.taskLoop_, is(sameInstance(taskLoop_)));
    }

    @Test
    public void testAddAfter_KeyAlreadyExists() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
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
        sut_.add(key, PipelineElement.newNullStage());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be added.");

        sut_.addAfter(key, StageKeys.of("IO_STAGE_KEY"), PipelineElement.newNullStage());
    }

    @Test
    public void testAddAfter_BaseKeyMustNotBeIOStage() throws Exception {
        StageKey key = StageKeys.of(0);
        sut_.add(key, PipelineElement.newNullStage());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must be the tail of this pipeline.");

        sut_.addAfter(StageKeys.of("IO_STAGE_KEY"), key, PipelineElement.newNullStage());
    }

    @Test
    public void testReplace_NullTaskLoopGroupReplacesToDefault() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.replace(key0, key1, stage1, null);

        @SuppressWarnings("unchecked")
        PipelineElement pe = (PipelineElement) sut_.searchElement(key1);
        assertThat(pe.taskLoop_, is(sameInstance(taskLoop_)));
    }

    @Test
    public void testRemove_First() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.remove(key0);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.remove(key1);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.remove(key2);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
        StageKey key3 = StageKeys.of(3);
        Stage stage3 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key0, key3, stage3);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
        StageKey key3 = StageKeys.of(3);
        Stage stage3 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key1, key3, stage3);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
        StageKey key3 = StageKeys.of(3);
        Stage stage3 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);
        sut_.add(key2, stage2);
        sut_.replace(key2, key3, stage3);

        Iterator<PipelineElement> i = sut_.iterator();
        PipelineElement context = i.next();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
        StageKey key3 = StageKeys.of(3);
        Stage stage3 = PipelineElement.newNullStage();
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
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();
        StageKey key2 = StageKeys.of(2);
        Stage stage2 = PipelineElement.newNullStage();
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
        sut_.add(key, PipelineElement.newNullStage());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be added.");

        sut_.replace(key, StageKeys.of("IO_STAGE_KEY"), PipelineElement.newNullStage());
    }

    @Test
    public void testReplace_OldIOStageIsRejected() throws Exception {
        StageKey key = StageKeys.of(0);
        sut_.add(key, PipelineElement.newNullStage());

        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("StringStageKey:IO_STAGE_KEY must not be removed.");

        sut_.replace(StageKeys.of("IO_STAGE_KEY"), key, PipelineElement.newNullStage());
    }

    @Test
    public void testSearchContext() throws Exception {
        StageKey key0 = StageKeys.of(0);
        Stage stage0 = PipelineElement.newNullStage();
        StageKey key1 = StageKeys.of(1);
        Stage stage1 = PipelineElement.newNullStage();

        sut_.add(key0, stage0);
        sut_.add(key1, stage1);

        PipelineElement context0 = sut_.searchElement(key0);
        PipelineElement context1 = sut_.searchElement(key1);
        PipelineElement context2 = sut_.searchElement(PipelineImpl.LAST);
        assertThat(context0.key(), is(key0));
        assertThat(context1.key(), is(key1));
        assertThat(context2.key(), is(PipelineImpl.LAST));

    }

    @Test
    public void testActivate() throws Exception {
        Stage s0 = spy(PipelineElement.newNullStage());
        Stage s1 = spy(PipelineElement.newNullStage());
        sut_.add(StageKeys.of(0), s0);
        sut_.add(StageKeys.of(1), s1);
        when(taskLoop_.isInLoopThread()).thenReturn(true);

        sut_.activate();

        verify(s0).activated(Mockito.<StageContext>any());
        verify(s1).activated(Mockito.<StageContext>any());
    }

    @Test
    public void testDeactivate() throws Exception {
        Stage s0 = spy(PipelineElement.newNullStage());
        Stage s1 = spy(PipelineElement.newNullStage());
        sut_.add(StageKeys.of(0), s0);
        sut_.add(StageKeys.of(1), s1);
        when(taskLoop_.isInLoopThread()).thenReturn(true);

        sut_.deactivate(Pipeline.DeactivateState.WHOLE);

        verify(s0).deactivated(Mockito.<StageContext>any(), eq(Pipeline.DeactivateState.WHOLE));
        verify(s1).deactivated(Mockito.<StageContext>any(), eq(Pipeline.DeactivateState.WHOLE));
    }

    @Test
    public void testCatchException() throws Exception {
        Exception e = new Exception();
        Stage s0 = mock(Stage.class);
        Stage s1 = mock(Stage.class);
        sut_.add(StageKeys.of(0), s0);
        sut_.add(StageKeys.of(1), s1);
        when(taskLoop_.isInLoopThread()).thenReturn(true);

        sut_.catchException(e);

        verify(s0).exceptionCaught(Mockito.<StageContext>any(), eq(e));
        verify(s1).exceptionCaught(Mockito.<StageContext>any(), eq(e));
    }

    @Test
    public void testToString_NoStageIsAdded() throws Exception {
        assertThat(sut_.toString(), is("[(StringStageKey:LAST=" + PipelineImpl.LAST_STAGE + ")]"));
    }

    @Test
    public void testToString_ThreeStages() throws Exception {
        Stage one = PipelineElement.newNullStage();
        Stage two = PipelineElement.newNullStage();
        sut_.add(StageKeys.of(1), one);
        sut_.add(StageKeys.of(2), two);
        assertThat(sut_.toString(), is("[(IntStageKey:1=" + one
                + "), (IntStageKey:2=" + two
                + "), (StringStageKey:LAST=" + PipelineImpl.LAST_STAGE
                + ")]"));
    }

    private static class PipelineImpl extends DefaultPipeline<TaskLoop> {

        static final StageKey LAST = StageKeys.of("LAST");
        static final Stage LAST_STAGE = PipelineElement.newNullStage();

        @SuppressWarnings("unchecked")
        protected PipelineImpl(AbstractTransport<TaskLoop> transport, TaskLoopGroup<TaskLoop> taskLoopGroup) {
            super("test", transport, taskLoopGroup, LAST, LAST_STAGE);
        }

        @Override
        PipelineElement createContext(
                StageKey key, final Stage stage, TaskLoopGroup<? extends TaskLoop> pool) {
            // return spy(new PipelineElement(this, key, stage, pool));
            return new PipelineElement(this, key, stage, pool);
        }
    }
}
