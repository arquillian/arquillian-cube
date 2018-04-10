package org.arquillian.cube.requirement;

import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

public class SystemPropertyOrEnvironmentVariableRequirement
    implements Constraint<RequiresSystemPropertyOrEnvironmentVariable> {

    public static String convertSystemPropertyNameToEnvVar(String systemPropertyName) {
        return systemPropertyName.toUpperCase().replaceAll("[.-]", "_");
    }

    @Override
    public void check(RequiresSystemPropertyOrEnvironmentVariable context) throws UnsatisfiedRequirementException {
        if (context != null) {
            for (String key : context.value()) {

                if (System.getProperties().containsKey(key)) {
                    continue;
                }

                String environmentVariable = convertSystemPropertyNameToEnvVar(key);
                if (!System.getenv().containsKey(environmentVariable)) {
                    throw new UnsatisfiedRequirementException("Neither system property with key: ["
                        + key
                        + "], nor environment variable with key:["
                        + environmentVariable
                        + "] found!");
                }
            }
        }
    }
}
