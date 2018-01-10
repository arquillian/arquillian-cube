package org.arquillian.cube.requirement;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;
import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.Requires;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

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
                Requirement requirement;
                if (requirementType.isInterface()) {
                    requirement = loadRequirement(context);
                } else {
                    requirement = requirementType.newInstance();
                }
                requirement.check(context);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Loading first found implementation of requirement found on classpath assuming
    // we have only one implementation on classpath.
    private static Requirement loadRequirement(Annotation context) {
        Requirement requirement = null;
        final ServiceLoader<Requirement> requirements = ServiceLoader.load(Requirement.class);

        for (Requirement req : requirements) {
            try {
                req.getClass().getDeclaredMethod("check", context.annotationType());
                requirement = req;
                break;
            } catch (NoSuchMethodException e) {
                // Look for next implementation if method not found with required signature.
            }
        }

        if (requirement == null) {
            throw new IllegalStateException("Couldn't found any implementation of " + Requirement.class);
        }
        return requirement;
    }
}
