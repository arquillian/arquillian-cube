package org.arquillian.cube.spi;

import java.util.Map;

/**
 * Interface that represents all the cubes that are going to autostarted by Cube.
 * This interface does not provide any order, just selects which ones are autostarted.
 */
public interface AutoStartParser {

    Map<String, Node> parse();
}
