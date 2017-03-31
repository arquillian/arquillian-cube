package org.arquillian.cube.requirement;

import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

public class EnvironmentVariableRequirement implements Requirement<RequiresEnvironmentVariable> {

    @Override
    public void check(RequiresEnvironmentVariable context) throws UnsatisfiedRequirementException {
        if (context != null) {
            for (String key : context.value()) {
                if (!System.getenv().containsKey(key)) {
                    throw new UnsatisfiedRequirementException("No environment variable with key: ["+ key +"] found");
                }
            }
        }
    }
}
