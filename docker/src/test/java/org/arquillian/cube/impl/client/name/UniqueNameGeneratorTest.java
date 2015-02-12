package org.arquillian.cube.impl.client.name;


import org.junit.Test;

import static org.junit.Assert.*;


public class UniqueNameGeneratorTest {

    @Test
    public void nameGeneration(){

        final String prefix = "prefix";
        final String name = "name";

        final UniqueNameGenerator generator = new UniqueNameGenerator( prefix );


        final String returned = generator.getName( name );

        final String expectedPrefix = prefix + "_";

        assertTrue( "Prefix correct",  returned.startsWith( expectedPrefix ));

        final String nameSection = returned.replace( expectedPrefix, "" );


        final String expectedName = name + "_";

        assertTrue("Name correct", nameSection.startsWith( expectedName ));

        final String uuidSection = nameSection.replace( expectedName, "" );

        //32 hex characters + 4 '-' chars
        assertEquals("uuid section correct", 36, uuidSection.length());
    }



    @Test(expected = IllegalArgumentException.class)
    public void noPrefixIllegal(){
        //throws an illegal argument
        new UniqueNameGenerator( null );
    }
}
