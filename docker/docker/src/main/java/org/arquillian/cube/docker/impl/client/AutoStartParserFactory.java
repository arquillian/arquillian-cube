package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.spi.AutoStartParser;
import org.jboss.arquillian.core.api.Injector;

public class AutoStartParserFactory {

    public static AutoStartParser create(String expression, DockerCompositions containersDefinition, Injector injector) {
        if (isNone(expression)) {
            return new NoneAutoStartParser();
        } else {
            if (isRegularExpressionBased(expression)) {
                return new RegularExpressionAutoStartParser(expression, containersDefinition);
            } else {
                if (isCustomImplementation(expression)) {
                    return new CustomAutoStartParser(injector,
                            expression.substring(
                                    expression.indexOf(CustomAutoStartParser.CUSTOM_PREFIX) + CustomAutoStartParser.CUSTOM_PREFIX.length()).trim()
                            );
                } else {
                    if (isCommaSeparated(expression)) {
                        return new CommaSeparatedAutoStartParser(expression, containersDefinition);
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    private static boolean isCustomImplementation(String expression) {
        return expression != null && expression.startsWith(CustomAutoStartParser.CUSTOM_PREFIX);
    }

    private static boolean isNone(String expression) {
        return expression != null && "[none]".equals(expression.trim());
    }

    private static boolean isCommaSeparated(String expression) {
        return expression != null;
    }


    private static boolean isRegularExpressionBased(String expression) {
        return expression != null && expression.startsWith(RegularExpressionAutoStartParser.REGULAR_EXPRESSION_PREFIX);
    }
}
