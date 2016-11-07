package org.arquillian.cube.docker.impl.client.utils;

import org.junit.Test;

import static org.arquillian.cube.docker.impl.client.utils.NumberConversion.convertToLong;
import static org.arquillian.cube.docker.impl.client.utils.NumberConversion.humanReadableByteCount;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NumberConversionTest {

    private static final Long TEN_MEGABYTES = 9999800L;

    private static final Long ONE_KIKIBYTE = 1024L;

    private static final Long NEGATIVE_ONE_KIKIBYTE = -1024L;

    private static final Long ONE_TERABYTES = 1000000000000L;

    private static final Long HUNDRED_GIGABYTES = 100000000000L;

    private static final Long ONE_EXBIBYTE =  1152921504606846976L;

    private static final Long ONE_PEBIBYTE = 1125899906842624L;

    @Test
    public void should_be_convert_byte_to_long(){
        assertThat(convertToLong((byte)127), is(127L));
    }

    @Test
    public void should_be_convert_short_to_long(){
        assertThat(convertToLong((short) 32767), is(32767L));
    }

    @Test
    public void should_be_convert_integer_to_long(){
        assertThat(convertToLong(2147483647), is(2147483647L));
    }

    @Test
    public void should_be_convert_hex_to_long(){
        assertThat(convertToLong(0x10), is(16L));
    }

    @Test
    public void  should_be_return_zero_to_null(){
        assertThat(convertToLong(null), is(0L));
    }

    @Test
    public void should_be_possible_to_show_null_as_zero_bytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(null, true), is("0 B"));
    }

    @Test
    public void should_be_possible_to_show_megabytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(TEN_MEGABYTES, true), is("10.00 MB"));
    }

    @Test
    public void should_be_possible_to_show_gigabytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(HUNDRED_GIGABYTES, true), is("100.00 GB"));
    }

    @Test
    public void should_be_possible_to_show_terabytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(ONE_TERABYTES, true), is("1.00 TB"));
    }


    @Test
    public void should_be_possible_to_show_kikibytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(ONE_KIKIBYTE, false), is("1.00 KiB"));
    }

    @Test
    public void should_be_possible_to_show_pebibytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(ONE_PEBIBYTE, false), is("1.00 PiB"));
    }

    @Test
    public void should_be_possible_to_show_exabytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(ONE_EXBIBYTE, false), is("1.00 EiB"));
    }

    @Test
    public void should_be_possible_to_show_negative_kikibytes_in_human_readable_form(){
        assertThat(humanReadableByteCount(NEGATIVE_ONE_KIKIBYTE, false), is("-1.00 KiB"));
    }
}
