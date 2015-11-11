package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil.Node;

import java.util.Map;

public interface AutoStartParser {

    Map<String, Node> parse();
}
