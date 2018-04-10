package org.arquillian.cube.requirement;

import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.spi.requirement.Requires;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import static org.arquillian.cube.requirement.Constraints.checkConstraint;

public class ArquillianConditionalRunner extends Arquillian {

    private static final Logger log = Logger.getLogger(ArquillianConditionalRunner.class.getName());

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
            log.log(Level.WARNING,
                String.format("Unsatisfied assumption in test class %s. Requirement problem: %s.", testClass.getName(),
                    e.getMessage()));
            notifier.fireTestAssumptionFailed(new Failure(getDescription(), e));
        }
    }

    private void checkRequirements(Class<?> testClass) throws UnsatisfiedRequirementException {
        //Check if Requires is used directly.
        checkConstraint(testClass.getAnnotation(Requires.class), null);

        for (Annotation annotation : testClass.getAnnotations()) {
            //Check if Requires is annotating an other annotation
            checkConstraint(annotation.annotationType().getAnnotation(Requires.class), annotation);
        }
    }
}
