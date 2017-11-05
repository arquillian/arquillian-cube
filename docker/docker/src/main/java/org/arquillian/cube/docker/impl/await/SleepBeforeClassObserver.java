package org.arquillian.cube.docker.impl.await;

import org.arquillian.cube.Sleep;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.impl.util.Timespan;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import java.util.concurrent.TimeUnit;

public class SleepBeforeClassObserver {

    public void executeSleep(@Observes BeforeClass beforeClass) {
        final TestClass testClass = beforeClass.getTestClass();
        if (ReflectionUtil.isClassWithAnnotation(testClass.getJavaClass(), Sleep.class)) {

            final Sleep sleep = testClass.getAnnotation(Sleep.class);
            executeSleep(sleep);

        }
    }

    private void executeSleep(Sleep sleep) {
        final long milliseconds = Timespan.toMilliseconds(sleep.value());
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
