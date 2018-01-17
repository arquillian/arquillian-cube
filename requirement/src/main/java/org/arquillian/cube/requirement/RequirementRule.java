package org.arquillian.cube.requirement;

import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.spi.requirement.Requires;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.arquillian.cube.requirement.Constraints.checkConstraint;

public class RequirementRule implements TestRule {

    private static final Logger log = Logger.getLogger(RequirementRule.class.getName());

    @Override
    public Statement apply(Statement base, Description description) {
        Statement result = base;
        try {
            //Check if Requires is used directly.
            checkConstraint(description.getAnnotation(Requires.class), null);
            for (Annotation annotation : description.getAnnotations()) {
                //Check if Requires is annotating an other annotation
                checkConstraint(annotation.annotationType().getAnnotation(Requires.class), annotation);
            }
        } catch (UnsatisfiedRequirementException e) {
            log.log(Level.WARNING, String.format("Unsatisfied assumption in test class %s. Requirement problem: %s.",
                description.getTestClass().getName(), e.getMessage()));
            return new UnsatisfiedRequirement(e.getMessage());
        }
        return result;
    }
}
