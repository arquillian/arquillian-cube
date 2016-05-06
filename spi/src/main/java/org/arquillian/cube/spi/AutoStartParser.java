package org.arquillian.cube.spi;

import java.util.Map;

public interface AutoStartParser {

    Map<String, Node> parse();
}
