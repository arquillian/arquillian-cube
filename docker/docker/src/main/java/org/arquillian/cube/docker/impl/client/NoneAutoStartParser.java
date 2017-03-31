package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;
import org.arquillian.cube.spi.AutoStartParser;
import org.arquillian.cube.spi.Node;

import java.util.HashMap;
import java.util.Map;

public class NoneAutoStartParser implements AutoStartParser {
    @Override
    public Map<String, Node> parse() {
        return new HashMap<>();
    }
}
