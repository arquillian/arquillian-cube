package org.arquillian.cube.requirement;

import static org.arquillian.cube.requirement.Requirements.checkRequirement;

import org.arquillian.cube.spi.requirement.Requires;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;


public class RequirementRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        Statement result = base;
        try {
            //Check if Requires is used directly.
            checkRequirement(description.getAnnotation(Requires.class), null);
            for (Annotation annotation : description.getAnnotations()) {
                //Check if Requires is annotating an other annotation
                checkRequirement(annotation.annotationType().getAnnotation(Requires.class), annotation);
            }
        } catch (UnsatisfiedRequirementException e) {
            return new UnsatisfiedRequirement(e.getMessage());
        }
        return result;
    }



}
