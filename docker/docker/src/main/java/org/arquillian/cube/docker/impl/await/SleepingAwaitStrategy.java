package org.arquillian.cube.docker.impl.await;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.spi.Cube;

public class SleepingAwaitStrategy implements AwaitStrategy {

    private static final Logger log = Logger.getLogger(SleepingAwaitStrategy.class.getName());

    public static final String TAG = "sleeping";

    private static final int DEFAULT_SLEEP_TIME = 500;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private int sleepTime = DEFAULT_SLEEP_TIME;
    private TimeUnit timeUnit = DEFAULT_TIME_UNIT;

    public SleepingAwaitStrategy(Cube<?> cube, Await params) {
        if (params.getSleepTime() != null) {
            configureSleepingTime(params.getSleepTime());
        }
    }

    private void configureSleepingTime(Object sleepTime) {
        if(sleepTime instanceof Integer) {
            this.sleepTime = (Integer) sleepTime;
        } else {
            String sleepTimeWithUnit = ((String) sleepTime).trim();
            if(sleepTimeWithUnit.endsWith("ms")) {
                this.timeUnit = TimeUnit.MILLISECONDS;
            } else {
                if(sleepTimeWithUnit.endsWith("s")) {
                    this.timeUnit = TimeUnit.SECONDS;
                    this.sleepTime = Integer.parseInt(sleepTimeWithUnit.substring(0, sleepTimeWithUnit.indexOf('s')).trim());
                } else {
                    this.timeUnit = TimeUnit.MILLISECONDS;
                    this.sleepTime = Integer.parseInt(sleepTimeWithUnit.substring(0, sleepTimeWithUnit.indexOf("ms")).trim());
                }
            }
        }
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public boolean await() {
        try {
            timeUnit.sleep(sleepTime);
        } catch (final InterruptedException e) {
            log.log(Level.WARNING, e.getMessage());
        }
        return true;
    }
}
