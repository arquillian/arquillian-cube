package org.arquillian.cube.docker.impl.await;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import org.apache.commons.lang3.StringUtils;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.await.AwaitStrategy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class SleepingAwaitStrategyBase implements AwaitStrategy {

    private static final int DEFAULT_SLEEP_TIME = 250;

    private int sleepTime;

    private TimeUnit timeUnit;

    protected SleepingAwaitStrategyBase(Object sleepTime) {
        this(sleepTime, DEFAULT_SLEEP_TIME);
    }

    protected SleepingAwaitStrategyBase(Object sleepTime, int defaultSleepTime) {
        configureSleepingTime(sleepTime != null ? sleepTime : defaultSleepTime);
    }

    private void configureSleepingTime(Object sleepTime) {
        if (sleepTime instanceof Integer) {
            this.timeUnit = TimeUnit.MILLISECONDS;
            this.sleepTime = (Integer) sleepTime;
        } else {
            String sleepTimeWithUnit = ((String) sleepTime).trim();
            if (sleepTimeWithUnit.endsWith("ms")) {
                this.timeUnit = TimeUnit.MILLISECONDS;
                this.sleepTime = Integer.parseInt(sleepTimeWithUnit.substring(0, sleepTimeWithUnit.indexOf("ms")).trim());
            } else if (sleepTimeWithUnit.endsWith("s")) {
                this.timeUnit = TimeUnit.SECONDS;
                this.sleepTime = Integer.parseInt(sleepTimeWithUnit.substring(0, sleepTimeWithUnit.indexOf('s')).trim());
            } else {
                this.timeUnit = TimeUnit.MILLISECONDS;
                this.sleepTime = Integer.parseInt(sleepTimeWithUnit);
            }
        }
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

}
