package net.ihiroky.niotty.buffer;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Hiroki Itoh
 */
public class CodecUtilTest {

    @Test
    public void testVariableByteLength() throws Exception {
        assertThat(CodecUtil.variableByteLength(63), is(1));
        assertThat(CodecUtil.variableByteLength(64), is(2)); // 2^6
        assertThat(CodecUtil.variableByteLength(8191), is(2));
        assertThat(CodecUtil.variableByteLength(8192), is(3)); // 2^(6+7)
        assertThat(CodecUtil.variableByteLength(1048575), is(3));
        assertThat(CodecUtil.variableByteLength(1048576), is(4)); // 2^(6+7+7)
        assertThat(CodecUtil.variableByteLength(134217727), is(4));
        assertThat(CodecUtil.variableByteLength(134217728), is(5)); // 2^(6+7+7+7)
        assertThat(CodecUtil.variableByteLength(Integer.MAX_VALUE), is(5));

        assertThat(CodecUtil.variableByteLength(-63), is(1));
        assertThat(CodecUtil.variableByteLength(-64), is(2));
        assertThat(CodecUtil.variableByteLength(-8191), is(2));
        assertThat(CodecUtil.variableByteLength(-8192), is(3));
        assertThat(CodecUtil.variableByteLength(-1048575), is(3));
        assertThat(CodecUtil.variableByteLength(-1048576), is(4));
        assertThat(CodecUtil.variableByteLength(-134217727), is(4));
        assertThat(CodecUtil.variableByteLength(-134217728), is(5));
        assertThat(CodecUtil.variableByteLength(-Integer.MAX_VALUE), is(5));
        assertThat(CodecUtil.variableByteLength(Integer.MIN_VALUE), is(5));
    }
}
