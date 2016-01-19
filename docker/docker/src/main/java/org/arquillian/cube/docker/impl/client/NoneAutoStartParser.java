package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;

import java.util.HashMap;
import java.util.Map;

public class NoneAutoStartParser implements AutoStartParser {
    @Override
    public Map<String, AutoStartOrderUtil.Node> parse() {
        return new HashMap<>();
    }
}
