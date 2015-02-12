package org.arquillian.cube.impl.client.name;


import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.impl.client.CubeConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 *
 */
public class NameGeneratorFactoryTest {

    @Test
    public void unspecifiedUsesStaticName() {
        final CubeConfiguration configuration = new CubeConfiguration();

        final NameGenerator generator = NameGeneratorFactory.getGenerator( configuration );

        assertTrue( "Correct instance returned", generator instanceof StaticNameGenerator );
    }


    @Test
    public void staticConfiguration() {

        final Map<String, String> options = new HashMap<String, String>() {{
            put( "nameGenerator", "static" );
        }};
        final CubeConfiguration configuration = CubeConfiguration.fromMap( options );

        final NameGenerator generator = NameGeneratorFactory.getGenerator( configuration );

        assertTrue( "Correct instance returned", generator instanceof StaticNameGenerator );
    }


    @Test
    public void uniqueConfiguration() {

        final Map<String, String> options = new HashMap<String, String>() {{
            put( "nameGenerator", "unique" );
            put( "nameGeneratorPrefix", "test" );
        }};

        final CubeConfiguration configuration = CubeConfiguration.fromMap( options );

        final NameGenerator generator = NameGeneratorFactory.getGenerator( configuration );

        assertTrue( "Correct instance returned", generator instanceof UniqueNameGenerator );
    }


    @Test( expected = IllegalArgumentException.class )
    public void uniqueConfigurationNoPrefix() {

        final Map<String, String> options = new HashMap<String, String>() {{
            put( "nameGenerator", "unique" );
        }};

        final CubeConfiguration configuration = CubeConfiguration.fromMap( options );

        //should throw illegal argument
        NameGeneratorFactory.getGenerator( configuration );
    }


    @Test( expected = IllegalArgumentException.class )
    public void invalidTypeName() {

        final Map<String, String> options = new HashMap<String, String>() {{
            put( "nameGenerator", "random stuff" );
        }};

        final CubeConfiguration configuration = CubeConfiguration.fromMap( options );

        //should throw illegal argument, not a type we support
        NameGeneratorFactory.getGenerator( configuration );
    }
}
