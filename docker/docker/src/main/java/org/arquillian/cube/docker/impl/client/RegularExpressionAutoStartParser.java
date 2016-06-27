package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;
import org.arquillian.cube.spi.AutoStartParser;
import org.arquillian.cube.spi.Node;

public class RegularExpressionAutoStartParser implements AutoStartParser {

    public static final String REGULAR_EXPRESSION_PREFIX = "regexp:";

    private String expression;
    private DockerCompositions containerDefinitions;

    public RegularExpressionAutoStartParser(String expression, DockerCompositions containerDefinitions) {
        if(!expression.startsWith(REGULAR_EXPRESSION_PREFIX)) {
            throw new IllegalArgumentException("Regular Expression AutoStartParser should begin with "+REGULAR_EXPRESSION_PREFIX);
        }
        this.expression = expression;
        this.containerDefinitions = containerDefinitions;
    }

    @Override
    public Map<String, Node> parse() {
        Map<String, Node> nodes = new HashMap<>();

        String regularExpression = getRegularExpression(expression);
        Pattern pattern = Pattern.compile(regularExpression);
        Set<String> definedContainers = containerDefinitions.getNoneManualContainers().keySet();
        for(String containerName : definedContainers) {
            Matcher matcher = pattern.matcher(containerName);
            if(matcher.matches()) {
                nodes.put(containerName, Node.from(containerName));
            }
        }
        return nodes;
    }

    private String getRegularExpression(String autoStartContainer) {
        return autoStartContainer.substring(autoStartContainer.indexOf(':') + 1).trim();
    }

    @Override
    public String toString() {
        return AutoStartOrderUtil.toString(parse());
    }
}
