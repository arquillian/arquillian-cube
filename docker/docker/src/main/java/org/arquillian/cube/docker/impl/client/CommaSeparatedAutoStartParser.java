package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;
import org.arquillian.cube.docker.impl.util.ConfigUtil;

public class CommaSeparatedAutoStartParser implements AutoStartParser {

    private String expression;
    private Map<String, Object> containerDefinitions;

    public CommaSeparatedAutoStartParser(String expression, Map<String, Object> containerDefinitions) {
        this.expression = expression;
        this.containerDefinitions = containerDefinitions;
    }

    @Override
    public Map<String, AutoStartOrderUtil.Node> parse() {
        Map<String, AutoStartOrderUtil.Node> nodes = new HashMap<>();

        String[] autoStartContainers = ConfigUtil.trim(expression.split(","));
        for (String autoStart : autoStartContainers) {
            if (containerDefinitions.containsKey(autoStart)) {
                nodes.put(autoStart, AutoStartOrderUtil.Node.from(autoStart));
            }
        }
        return nodes;
    }

    @Override
    public String toString() {
        return AutoStartOrderUtil.toString(parse());
    }
}
