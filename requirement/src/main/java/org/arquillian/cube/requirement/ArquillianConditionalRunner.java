package org.arquillian.cube.requirement;

import org.arquillian.cube.spi.requirement.Requires;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.lang.annotation.Annotation;

import static org.arquillian.cube.requirement.Requirements.checkRequirement;

public class ArquillianConditionalRunner extends Arquillian {

    public ArquillianConditionalRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    public void run(RunNotifier notifier) {
        Class testClass = getTestClass().getJavaClass();
        try {
            checkRequirements(testClass);
            super.run(notifier);
        } catch (UnsatisfiedRequirementException e) {
            notifier.fireTestAssumptionFailed(new Failure(getDescription(), e));
        }
    }

    private void checkRequirements(Class<?> testClass) throws UnsatisfiedRequirementException {
            //Check if Requires is used directly.
            checkRequirement(testClass.getAnnotation(Requires.class), null);

            for (Annotation annotation : testClass.getAnnotations()) {
                //Check if Requires is annotating an other annotation
                checkRequirement(annotation.annotationType().getAnnotation(Requires.class), annotation);
            }
    }
}
