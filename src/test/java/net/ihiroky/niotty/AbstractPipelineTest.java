package net.ihiroky.niotty;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class AbstractPipelineTest {

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testAdd() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();

        sut.add(key0, stage0);
        sut.add(key1, stage1);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testAdd_KeyAlreadyExists() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("key IntStageKey:1 already exists.");

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key1, stage1);
    }

    @Test
    public void testAddBefore() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut.add(key0, stage0);
        sut.addBefore(key0, key1, stage1);
        sut.addBefore(key0, key2, stage2);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testAddBefore_KeyAlreadyExists() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("key IntStageKey:0 already exists.");

        sut.add(key0, stage0);
        sut.addBefore(key0, key1, stage1);
        sut.addBefore(key0, key2, stage2);
        sut.addBefore(key0, key0, stage0);
    }

    @Test
    public void testAddBefore_NoBaseKeyFound() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        exceptionRule_.expect(NoSuchElementException.class);
        exceptionRule_.expectMessage("baseKey IntStageKey:3 is not found.");

        sut.add(key0, stage0);
        sut.addBefore(key0, key1, stage1);
        sut.addBefore(key3, key2, stage2);
    }

    @Test
    public void testAddAfter() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut.add(key0, stage0);
        sut.addAfter(key0, key1, stage1);
        sut.addAfter(key0, key2, stage2);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testAddAfter_KeyAlreadyExists() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("key IntStageKey:0 already exists.");

        sut.add(key0, stage0);
        sut.addAfter(key0, key1, stage1);
        sut.addAfter(key0, key2, stage2);
        sut.addAfter(key0, key0, stage0);
    }

    @Test
    public void testAddAfter_NoBaseKeyFound() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        exceptionRule_.expect(NoSuchElementException.class);
        exceptionRule_.expectMessage("baseKey IntStageKey:3 is not found.");

        sut.add(key0, stage0);
        sut.addAfter(key0, key1, stage1);
        sut.addAfter(key3, key2, stage2);
    }

    @Test
    public void testRemove_First() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.remove(key0);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testRemove_Middle() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.remove(key1);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testRemove_Last() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.remove(key2);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testRemove_NoKeyIsFound() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        exceptionRule_.expect(NoSuchElementException.class);
        exceptionRule_.expectMessage("key IntStageKey:2 is not found.");

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.remove(key2);
    }

    @Test
    public void testReplace_First() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        Object stage3 = new Object();

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.replace(key0, key3, stage3);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key3));
        assertThat(context.stage(), is(stage3));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testReplace_Middle() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        Object stage3 = new Object();

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.replace(key1, key3, stage3);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key3));
        assertThat(context.stage(), is(stage3));
        context = i.next();
        assertThat(context.key(), is(key2));
        assertThat(context.stage(), is(stage2));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testReplace_Last() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        Object stage3 = new Object();

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.replace(key2, key3, stage3);

        Iterator<PipelineElement<Object, Object>> i = sut.iterator();
        PipelineElement<Object, Object> context = i.next();
        assertThat(context.key(), is(key0));
        assertThat(context.stage(), is(stage0));
        context = i.next();
        assertThat(context.key(), is(key1));
        assertThat(context.stage(), is(stage1));
        context = i.next();
        assertThat(context.key(), is(key3));
        assertThat(context.stage(), is(stage3));
        assertThat(i.hasNext(), is(false));
    }

    @Test
    public void testReplace_OldKeyIsNotFound() throws Exception {
        PipelineImpl sut = new PipelineImpl();
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

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.replace(key3, key3, stage3);
    }

    @Test
    public void testReplace_NewKeyAlreadyExists() throws Exception {
        PipelineImpl sut = new PipelineImpl();
        StageKey key0 = StageKeys.of(0);
        Object stage0 = new Object();
        StageKey key1 = StageKeys.of(1);
        Object stage1 = new Object();
        StageKey key2 = StageKeys.of(2);
        Object stage2 = new Object();
        StageKey key3 = StageKeys.of(3);
        exceptionRule_.expect(IllegalArgumentException.class);
        exceptionRule_.expectMessage("newKey IntStageKey:2 already exists.");

        sut.add(key0, stage0);
        sut.add(key1, stage1);
        sut.add(key2, stage2);
        sut.replace(key3, key2, stage2);
    }

    private static class PipelineImpl extends AbstractPipeline<Object> {

        protected PipelineImpl() {
            super("test", null);
        }

        @Override
        protected PipelineElement<Object, Object> createContext(
                StageKey key, final Object stage, PipelineElementExecutorPool pool) {
            return new PipelineElement<Object, Object>(this, key, pool) {
                @Override
                protected Object stage() {
                    return stage;
                }
                @Override
                protected void fire(Object input) {
                }
                @Override
                protected void fire(AttachedMessage<Object> input) {
                }
                @Override
                protected void fire(TransportStateEvent event) {
                }
                @Override
                public Object attachment() {
                    return null;
                }
            };
        }
    }
}
