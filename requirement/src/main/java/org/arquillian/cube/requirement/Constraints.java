package org.arquillian.cube.requirement;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;
import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.Requires;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

class Constraints {

    private Constraints() {
        throw new UnsupportedOperationException("Utility Class");
    }

    static void checkConstraint(Requires requires, Annotation context) throws UnsatisfiedRequirementException {
        if (requires == null) {
            return;
        }
        for (Class<? extends Constraint> constraintType : requires.value()) {
            try {
                Constraint constraint;
                if (constraintType.isInterface()) {
                    constraint = loadConstraint(context);
                } else {
                    constraint = constraintType.newInstance();
                }
                constraint.check(context);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Loading first found implementation of constraint found on classpath assuming
    // we have only one implementation on classpath.
    private static Constraint loadConstraint(Annotation context) {
        Constraint constraint = null;
        final ServiceLoader<Constraint> constraints = ServiceLoader.load(Constraint.class);

        for (Constraint aConstraint : constraints) {
            try {
                aConstraint.getClass().getDeclaredMethod("check", context.annotationType());
                constraint = aConstraint;
                break;
            } catch (NoSuchMethodException e) {
                // Look for next implementation if method not found with required signature.
            }
        }

        if (constraint == null) {
            throw new IllegalStateException("Couldn't found any implementation of " + Constraint.class.getName());
        }
        return constraint;
    }
}
