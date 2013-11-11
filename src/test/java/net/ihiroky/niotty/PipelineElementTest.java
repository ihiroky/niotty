package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class PipelineElementTest {

    private PipelineElement sut_;
    private TaskLoop taskLoop_;
    private Stage stage_;
    private PipelineElement next_;
    private PipelineElement prev_;

    @Before
    public void setUp() throws Exception {
        taskLoop_ = mock(TaskLoop.class);
        @SuppressWarnings("unchecked")
        TaskLoopGroup<? extends TaskLoop> taskLoopGroup = mock(TaskLoopGroup.class);
        Pipeline pipeline = mock(Pipeline.class);
        stage_ = mock(Stage.class);
        when(taskLoopGroup.assign(Mockito.<TaskSelection>any())).thenReturn(taskLoop_);
        sut_ = new PipelineElement(pipeline, StageKeys.of("Test"), stage_, taskLoopGroup);
        next_ = mock(PipelineElement.class);
        prev_ = mock(PipelineElement.class);
        sut_.setNext(next_);
        sut_.setPrev(prev_);
    }

    @Test
    public void testStoreProceed() throws Exception {
        Object message = new Object();

        sut_.storeContext_.proceed(message);

        verify(next_).callStore(message);
    }

    @Test
    public void testLoadProceed() throws Exception {
        Object message = new Object();

        sut_.loadContext_.proceed(message);

        verify(prev_).callLoad(message);
    }

    @Test
    public void testStoreProceedWithParameter() throws Exception {
        Object message = new Object();
        Object parameter = new Object();
        PipelineElement.ParameterStoreStageContext sut =
                new PipelineElement.ParameterStoreStageContext(sut_, parameter);

        sut.proceed(message);

        verify(next_).callStore(message, parameter);
    }

    @Test
    public void testLoadProceedWithParameter() throws Exception {
        Object message = new Object();
        Object parameter = new Object();
        PipelineElement.ParameterLoadStageContext sut =
                new PipelineElement.ParameterLoadStageContext(sut_, parameter);

        sut.proceed(message);

        verify(prev_).callLoad(message, parameter);
    }

    @Test
    public void testCallStore_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();

        sut_.callStore(message);

        verify(stage_).stored(sut_.storeContext_, message);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testCallStore_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();

        sut_.callStore(message);

        verify(stage_, never()).stored(sut_.storeContext_, message);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).stored(sut_.storeContext_, message);
    }

    @Test
    public void testCallStoreWithParameter_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callStore(message, parameter);

        ArgumentCaptor<StageContext> contextCaptor = ArgumentCaptor.forClass(StageContext.class);
        verify(stage_).stored(contextCaptor.capture(), eq(message));
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
        assertThat(contextCaptor.getValue().parameter(), is(parameter));
    }

    @Test
    public void testCallStoreWithParameter_IndirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callStore(message, parameter);

        verify(stage_, never()).stored(Mockito.<StageContext>any(), eq(message));

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);

        ArgumentCaptor<StageContext> contextCaptor = ArgumentCaptor.forClass(StageContext.class);
        verify(stage_).stored(contextCaptor.capture(), eq(message));
        assertThat(contextCaptor.getValue().parameter(), is(parameter));
    }

    @Test
    public void testCallLoad_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();

        sut_.callLoad(message);

        verify(stage_).loaded(sut_.loadContext_, message);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testCallLoad_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();

        sut_.callLoad(message);

        verify(stage_, never()).loaded(sut_.loadContext_, message);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).loaded(sut_.loadContext_, message);
    }

    @Test
    public void testCallLoadWithParameter_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callLoad(message, parameter);

        ArgumentCaptor<StageContext> contextCaptor = ArgumentCaptor.forClass(StageContext.class);
        verify(stage_).loaded(contextCaptor.capture(), eq(message));
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
        assertThat(contextCaptor.getValue().parameter(), is(parameter));
    }

    @Test
    public void testCallLoadWithParameter_IndirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callLoad(message, parameter);

        verify(stage_, never()).loaded(Mockito.<StageContext>any(), eq(message));

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);

        ArgumentCaptor<StageContext> contextCaptor = ArgumentCaptor.forClass(StageContext.class);
        verify(stage_).loaded(contextCaptor.capture(), eq(message));
        assertThat(contextCaptor.getValue().parameter(), is(parameter));
    }

    @Test
    public void testActivate_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);

        sut_.callActivate();

        verify(stage_).activated(sut_.stateContext_);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testActivate_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);

        sut_.callActivate();

        verify(stage_, never()).activated(sut_.stateContext_);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).activated(sut_.stateContext_);
    }

    @Test
    public void testDeactivate_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);

        sut_.callDeactivate(Pipeline.DeactivateState.LOAD);

        verify(stage_).deactivated(sut_.stateContext_, Pipeline.DeactivateState.LOAD);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testDeactivate_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);

        sut_.callDeactivate(Pipeline.DeactivateState.LOAD);

        verify(stage_, never()).deactivated(sut_.stateContext_, Pipeline.DeactivateState.LOAD);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).deactivated(sut_.stateContext_, Pipeline.DeactivateState.LOAD);
    }

    @Test
    public void testCatchException_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        Exception e = new Exception();

        sut_.callCatchException(e);

        verify(stage_).exceptionCaught(sut_.stateContext_, e);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testCatchException_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        Exception e = new Exception();

        sut_.callCatchException(e);

        verify(stage_, never()).exceptionCaught(sut_.stateContext_, e);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).exceptionCaught(sut_.stateContext_, e);
    }
}
