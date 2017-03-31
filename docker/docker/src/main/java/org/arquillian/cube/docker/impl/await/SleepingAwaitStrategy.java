package org.arquillian.cube.docker.impl.await;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.spi.Cube;

public class SleepingAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "sleeping";
    private static final Logger log = Logger.getLogger(SleepingAwaitStrategy.class.getName());

    public SleepingAwaitStrategy(Cube<?> cube, Await params) {
        super(params.getSleepTime());
    }

    @Override
    public boolean await() {
        try {
            getTimeUnit().sleep(getSleepTime());
        } catch (final InterruptedException e) {
            log.log(Level.WARNING, e.getMessage());
        }
        return true;
    }
}
