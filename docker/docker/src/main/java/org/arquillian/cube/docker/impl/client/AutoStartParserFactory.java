package org.arquillian.cube.docker.impl.client;

import java.util.Map;

public class AutoStartParserFactory {

    public static AutoStartParser create(String expression, Map<String, Object> containersDefinition) {
        if(isRegularExpressionBased(expression)) {
            return new RegularExpressionAutoStartParser(expression, containersDefinition);
        } else {
            if(isCommaSeparated(expression)) {
                return new CommaSeparatedAutoStartParser(expression, containersDefinition);
            } else {
                return null;
            }
        }
    }

    private static boolean isCommaSeparated(String expression) {
        return expression != null;
    }


    private static boolean isRegularExpressionBased(String expression) {
        return expression != null && expression.startsWith(RegularExpressionAutoStartParser.REGULAR_EXPRESSION_PREFIX);
    }
}
