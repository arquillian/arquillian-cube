package org.arquillian.cube.docker.impl.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.spi.Cube;

public class StaticAwaitStrategy implements AwaitStrategy {

    public static final String TAG = "static";

    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final int DEFAULT_SLEEP_POLL_TIME = 500;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private int sleepPollTime = DEFAULT_SLEEP_POLL_TIME;
    private TimeUnit timeUnit = DEFAULT_TIME_UNIT;

    private String ip;
    private List<Integer> ports = new ArrayList<Integer>();

    public StaticAwaitStrategy(Cube cube, Await params) {
        this.ip = params.getIp();
        this.ports.addAll(params.getPorts());

        if (params.getSleepPollingTime() != null) {
            configurePollingTime(params.getSleepPollingTime());
        }

        if (params.getIterations() != null) {
            this.pollIterations = params.getIterations();
        }
    }

    private void configurePollingTime(Object sleepTime) {
        if(sleepTime instanceof Integer) {
            this.sleepPollTime = (Integer) sleepTime;
        } else {
            String sleepTimeWithUnit = ((String) sleepTime).trim();
            if(sleepTimeWithUnit.endsWith("ms")) {
                this.timeUnit = TimeUnit.MILLISECONDS;
            } else {
                if(sleepTimeWithUnit.endsWith("s")) {
                    this.timeUnit = TimeUnit.SECONDS;
                    this.sleepPollTime = Integer.parseInt(sleepTimeWithUnit.substring(0, sleepTimeWithUnit.indexOf('s')).trim());
                } else {
                    this.timeUnit = TimeUnit.MILLISECONDS;
                    this.sleepPollTime = Integer.parseInt(sleepTimeWithUnit.substring(0, sleepTimeWithUnit.indexOf("ms")).trim());
                }
            }
        }
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public boolean await() {

        for (Integer port : this.ports) {
            if(!Ping.ping(this.ip, port, this.pollIterations, this.sleepPollTime, TimeUnit.MILLISECONDS )) {
                return false;
            }
        }

        return true;
    }

    public String getIp() {
        return ip;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public int getPollIterations() {
        return pollIterations;
    }

    public int getSleepPollTime() {
        return sleepPollTime;
    }
}
