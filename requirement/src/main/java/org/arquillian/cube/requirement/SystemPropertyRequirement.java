package org.arquillian.cube.requirement;

import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

public class SystemPropertyRequirement implements Requirement<RequiresSystemProperty> {

    @Override
    public void check(RequiresSystemProperty context) throws UnsatisfiedRequirementException {
        if (context != null) {
            for (String key : context.value()) {
                if (!System.getProperties().containsKey(key)) {
                    throw new UnsatisfiedRequirementException("No system property with key: [" + key + "] found");
                }
            }
        }
    }
}
