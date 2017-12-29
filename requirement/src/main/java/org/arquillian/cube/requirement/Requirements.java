package org.arquillian.cube.requirement;

import java.lang.annotation.Annotation;
import java.util.Iterator;
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
                    requirement = getRequirement(context);
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

    private static Requirement getRequirement(Annotation context) {
        Requirement requirement = null;

        final ServiceLoader<Requirement> requirementLoader = ServiceLoader.load(Requirement.class);

        final Iterator<Requirement> requirements = requirementLoader.iterator();

        while (requirement == null && requirements.hasNext()) {
            Requirement req = requirements.next();
            try {
                req.getClass().getDeclaredMethod("check", context.annotationType());
                requirement = req;
            } catch (NoSuchMethodException e) {
                // if method not found with required signature. Look for next implementation
            }
        }

        return requirement;
    }
}
