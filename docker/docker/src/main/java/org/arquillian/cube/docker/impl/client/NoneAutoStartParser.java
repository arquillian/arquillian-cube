package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.spi.AutoStartParser;
import org.arquillian.cube.spi.Node;

public class NoneAutoStartParser implements AutoStartParser {
    @Override
    public Map<String, Node> parse() {
        return new HashMap<>();
    }
}
