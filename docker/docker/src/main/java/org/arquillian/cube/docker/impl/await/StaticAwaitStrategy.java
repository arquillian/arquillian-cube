package org.arquillian.cube.docker.impl.await;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.spi.Cube;

public class StaticAwaitStrategy implements AwaitStrategy {

    private static final String PORTS = "ports";

    private static final String IP = "ip";

    public static final String TAG = "static";

    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final int DEFAULT_SLEEP_POLL_TIME = 500;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final String POLLING_TIME = "sleepPollingTime";
    private static final String ITERATIONS = "iterations";

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private int sleepPollTime = DEFAULT_SLEEP_POLL_TIME;
    private TimeUnit timeUnit = DEFAULT_TIME_UNIT;

    private String ip;
    private List<Integer> ports = new ArrayList<Integer>();

    @SuppressWarnings("unchecked")
    public StaticAwaitStrategy(Cube cube, Map<String, Object> params) {
        this.ip = (String) params.get(IP);
        this.ports.addAll((Collection<? extends Integer>) params.get(PORTS));

        if (params.containsKey(POLLING_TIME)) {
            configurePollingTime(params);
        }

        if (params.containsKey(ITERATIONS)) {
            this.pollIterations = (Integer) params.get(ITERATIONS);
        }
    }

    private void configurePollingTime(Map<String, Object> params) {
        Object sleepTime = params.get(POLLING_TIME);
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
