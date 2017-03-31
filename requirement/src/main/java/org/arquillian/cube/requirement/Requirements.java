package org.arquillian.cube.requirement;

import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.Requires;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

import java.lang.annotation.Annotation;

public class Requirements {

    private Requirements() {
        throw new UnsupportedOperationException("Utility Class");
    }

    public static void checkRequirement(Requires requires, Annotation context) throws UnsatisfiedRequirementException {
        if (requires == null) {
            return;
        }
        for (Class<? extends Requirement> requirementType : requires.value()) {
            try {
                Requirement requirement = requirementType.newInstance();
                requirement.check(context);
            }  catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }  catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
