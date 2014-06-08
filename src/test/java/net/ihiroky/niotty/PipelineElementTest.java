package net.ihiroky.niotty;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 *
 */
public class PipelineElementTest {

    private PipelineElement sut_;
    private EventDispatcherGroup<? extends EventDispatcher> eventDispatcherGroup_;
    private Pipeline pipeline_;
    private EventDispatcher eventDispatcher_;
    private Stage stage_;
    private PipelineElement next_;
    private PipelineElement prev_;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        eventDispatcher_ = mock(EventDispatcher.class);
        eventDispatcherGroup_ = mock(EventDispatcherGroup.class);
        pipeline_ = mock(Pipeline.class);
        stage_ = mock(Stage.class);
        when(eventDispatcherGroup_.assign(Mockito.<EventDispatcherSelection>any())).thenReturn(eventDispatcher_);
        sut_ = new PipelineElement(pipeline_, StageKeys.of("Test"), stage_, eventDispatcherGroup_);
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
    public void testCallStore_DirectlyIfInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(true);
        Object message = new Object();
        Object p = new Object();

        sut_.callStore(message, p);

        verify(stage_).stored(sut_.storeContext_, message, p);
        verify(eventDispatcher_, never()).offer(Mockito.<Event>any());
    }

    @Test
    public void testCallStore_IndirectlyIfNotInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(false);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callStore(message, parameter);

        verify(stage_, never()).stored(sut_.storeContext_, message, parameter);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher_).offer(eventCaptor.capture());
        eventCaptor.getValue().execute();
        verify(stage_).stored(sut_.storeContext_, message, parameter);
    }

    @Test
    public void testCallStore_ProceedsIfStageIsLoadStage() throws Exception {
        stage_ = mock(LoadStage.class);
        sut_ = new PipelineElement(pipeline_, StageKeys.of("Test"), stage_, eventDispatcherGroup_);
        sut_.setNext(next_);
        sut_.setPrev(prev_);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callStore(message, parameter);

        verify(sut_.next()).callStore(message, parameter);
    }

    @Test
    public void testCallLoad_DirectlyIfInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(true);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callLoad(message, parameter);

        verify(stage_).loaded(sut_.loadContext_, message, parameter);
        verify(eventDispatcher_, never()).offer(Mockito.<Event>any());
    }

    @Test
    public void testCallLoad_IndirectlyIfNotInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(false);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callLoad(message, parameter);

        verify(stage_, never()).loaded(sut_.loadContext_, message, parameter);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher_).offer(eventCaptor.capture());
        eventCaptor.getValue().execute();
        verify(stage_).loaded(sut_.loadContext_, message, parameter);
    }

    @Test
    public void testCallLoad_ProceedsIfStageIsStoreStage() throws Exception {
        stage_ = mock(StoreStage.class);
        sut_ = new PipelineElement(pipeline_, StageKeys.of("Test"), stage_, eventDispatcherGroup_);
        sut_.setNext(next_);
        sut_.setPrev(prev_);
        Object message = new Object();
        Object parameter = new Object();

        sut_.callLoad(message, parameter);

        verify(sut_.prev()).callLoad(message, parameter);
    }

    @Test
    public void testActivate_DirectlyIfInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(true);

        sut_.callActivate();

        verify(stage_).activated(sut_.stateContext_);
        verify(prev_).callActivate();
        verify(eventDispatcher_, never()).offer(Mockito.<Event>any());
    }

    @Test
    public void testActivate_IndirectlyIfNotInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(false);

        sut_.callActivate();

        verify(stage_, never()).activated(sut_.stateContext_);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher_).offer(eventCaptor.capture());
        eventCaptor.getValue().execute();
        verify(stage_).activated(sut_.stateContext_);
        verify(prev_).callActivate();
    }

    @Test
    public void testDeactivate_DirectlyIfInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(true);

        sut_.callDeactivate();

        verify(stage_).deactivated(sut_.stateContext_);
        verify(prev_).callDeactivate();
        verify(eventDispatcher_, never()).offer(Mockito.<Event>any());
    }

    @Test
    public void testDeactivate_IndirectlyIfNotInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(false);

        sut_.callDeactivate();

        verify(stage_, never()).deactivated(sut_.stateContext_);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher_).offer(eventCaptor.capture());
        eventCaptor.getValue().execute();
        verify(stage_).deactivated(sut_.stateContext_);
        verify(prev_).callDeactivate();
    }

    @Test
    public void testCatchException_DirectlyIfInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(true);
        Exception e = new Exception();

        sut_.callCatchException(e);

        verify(stage_).exceptionCaught(sut_.stateContext_, e);
        verify(prev_).callCatchException(e);
        verify(eventDispatcher_, never()).offer(Mockito.<Event>any());
    }

    @Test
    public void testCatchException_IndirectlyIfNotInDispatcherThread() throws Exception {
        when(eventDispatcher_.isInDispatcherThread()).thenReturn(false);
        Exception e = new Exception();

        sut_.callCatchException(e);

        verify(stage_, never()).exceptionCaught(sut_.stateContext_, e);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher_).offer(eventCaptor.capture());
        eventCaptor.getValue().execute();
        verify(stage_).exceptionCaught(sut_.stateContext_, e);
        verify(prev_).callCatchException(e);
    }
}
