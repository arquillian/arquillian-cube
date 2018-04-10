package org.arquillian.cube.impl;

import java.util.Map;
import org.jboss.arquillian.config.impl.extension.StringPropertyReplacer;

public class EnricherExpressionResolver {

    private ConfigurationParameters configurationParameters;

    public EnricherExpressionResolver(ConfigurationParameters configurationParameters) {
        this.configurationParameters = configurationParameters;
    }

    public String resolve(String property) {
        final String replacedProperties = StringPropertyReplacer.replaceProperties(property);
        if (!property.equals(replacedProperties)) {
            return replacedProperties;
        } else {
            final Map<String, String> configParams = configurationParameters.getConfigParams();
            final String propertyValue = configParams.get(propertyWithoutExpression(property));

            return propertyValue != null ? propertyValue : property;
        }
    }

    private String propertyWithoutExpression(String property) {
        if (property.startsWith("${") && property.endsWith("}")) {
            property = property.substring(2, property.length() - 1);
        }

        return property;
    }
}
