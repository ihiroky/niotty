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
        Object p = new Object();

        sut_.storeContext_.proceed(message, p);

        verify(next_).callStore(message, p);
    }

    @Test
    public void testLoadProceed() throws Exception {
        Object message = new Object();
        Object p = new Object();

        sut_.loadContext_.proceed(message, p);

        verify(prev_).callLoad(message, p);
    }

    @Test
    public void testCallStore_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();
        Object p = new Object();

        sut_.callStore(message, p);

        verify(stage_).stored(sut_.storeContext_, message, p);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testCallStore_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callStore(message, parameter);

        verify(stage_, never()).stored(sut_.storeContext_, message, parameter);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).stored(sut_.storeContext_, message, parameter);
    }

    @Test
    public void testCallLoad_DirectlyIfInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(true);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callLoad(message, parameter);

        verify(stage_).loaded(sut_.loadContext_, message, parameter);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testCallLoad_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callLoad(message, parameter);

        verify(stage_, never()).loaded(sut_.loadContext_, message, parameter);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).loaded(sut_.loadContext_, message, parameter);
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

        sut_.callDeactivate(DeactivateState.LOAD);

        verify(stage_).deactivated(sut_.stateContext_, DeactivateState.LOAD);
        verify(taskLoop_, never()).offer(Mockito.<Task>any());
    }

    @Test
    public void testDeactivate_IndirectlyIfNotInLoopThread() throws Exception {
        when(taskLoop_.isInLoopThread()).thenReturn(false);

        sut_.callDeactivate(DeactivateState.LOAD);

        verify(stage_, never()).deactivated(sut_.stateContext_, DeactivateState.LOAD);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskLoop_).offer(taskCaptor.capture());
        taskCaptor.getValue().execute(TimeUnit.MILLISECONDS);
        verify(stage_).deactivated(sut_.stateContext_, DeactivateState.LOAD);
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
