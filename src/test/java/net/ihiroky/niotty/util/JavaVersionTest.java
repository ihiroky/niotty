package net.ihiroky.niotty.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 *
 */
public class JavaVersionTest {

    @Rule
    public ExpectedException exceptionRule_ = ExpectedException.none();

    @Test
    public void testGe() throws Exception {
        assertThat(JavaVersion.JAVA7.ge(JavaVersion.JAVA7), is(true));
        assertThat(JavaVersion.JAVA7.ge(JavaVersion.JAVA6), is(true));
        assertThat(JavaVersion.JAVA6.ge(JavaVersion.JAVA7), is(false));
    }

    @Test
    public void testGt() throws Exception {
        assertThat(JavaVersion.JAVA7.gt(JavaVersion.JAVA7), is(false));
        assertThat(JavaVersion.JAVA7.gt(JavaVersion.JAVA6), is(true));
        assertThat(JavaVersion.JAVA6.gt(JavaVersion.JAVA7), is(false));
    }

    @Test
    public void testLe() throws Exception {
        assertThat(JavaVersion.JAVA7.le(JavaVersion.JAVA7), is(true));
        assertThat(JavaVersion.JAVA7.le(JavaVersion.JAVA6), is(false));
        assertThat(JavaVersion.JAVA6.le(JavaVersion.JAVA7), is(true));
    }

    @Test
    public void testLt() throws Exception {
        assertThat(JavaVersion.JAVA7.lt(JavaVersion.JAVA7), is(false));
        assertThat(JavaVersion.JAVA7.lt(JavaVersion.JAVA6), is(false));
        assertThat(JavaVersion.JAVA6.lt(JavaVersion.JAVA7), is(true));
    }

    @Test
    public void testThrowIfUnsupported() throws Exception {
        exceptionRule_.expect(UnsupportedOperationException.class);
        exceptionRule_.expectMessage("Java 7 or later is required.");
        JavaVersion.JAVA6.throwIfUnsupported(JavaVersion.JAVA7);
    }

    @Test
    public void testToString() throws Exception {
        assertThat(JavaVersion.JAVA8.toString(), is("Java 8"));
    }
}
