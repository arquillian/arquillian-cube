package org.arquillian.cube.docker.impl.client.utils;

import org.junit.Test;

import static org.arquillian.cube.docker.impl.client.utils.NumberConversion.convertToLong;
import static org.arquillian.cube.docker.impl.client.utils.NumberConversion.humanReadableByteCount;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NumberConversionTest {

    @Test
    public void convert_to_long_test() {
        assertThat(convertToLong((byte)127), is(127L));
        assertThat(convertToLong(null),is(0L));

        assertThat(convertToLong((short) 32767), is(32767L));
        assertThat(convertToLong(100L),is(100L));

        assertThat(convertToLong(2147483647), is(2147483647L));
        assertThat(convertToLong(0x10), is(16L));
    }

    @Test
    public void human_readable_byte_count(){
        assertThat(humanReadableByteCount(null, true), is("0 B"));
        assertThat(humanReadableByteCount(1024L, true), is("1.02 kB"));
        assertThat(humanReadableByteCount(1025L, false), is("1.00 KiB"));

        assertThat(humanReadableByteCount(9999800L, true), is("10.00 MB"));
        assertThat(humanReadableByteCount(281474976710656L,true), is("281.47 TB"));

        assertThat(humanReadableByteCount(8229401496703205376L, false), is("7.14 EiB"));
    }

}
