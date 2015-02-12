package org.arquillian.cube.impl.client.name;


import org.junit.Test;

import static org.junit.Assert.*;


public class StaticNameGeneratorTest {

    @Test
    public void testValidName(){
        final String expected = "testName";

        final StaticNameGenerator generator = new StaticNameGenerator();

        final String returned = generator.getName( expected );

        assertEquals( "Same string should be returned", expected, returned );

    }
}
