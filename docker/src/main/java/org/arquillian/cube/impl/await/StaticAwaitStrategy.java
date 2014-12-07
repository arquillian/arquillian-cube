package org.arquillian.cube.impl.await;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.impl.util.Ping;
import org.arquillian.cube.spi.Cube;

public class StaticAwaitStrategy implements AwaitStrategy {

    private static final String PORTS = "ports";

    private static final String IP = "ip";

    public static final String TAG = "static";

    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final int DEFAULT_SLEEP_POLL_TIME = 500;
    private static final String POLLING_TIME = "sleepPollingTime";
    private static final String ITERATIONS = "iterations";

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private int sleepPollTime = DEFAULT_SLEEP_POLL_TIME;

    private String ip;
    private List<Integer> ports = new ArrayList<Integer>();

    @SuppressWarnings("unchecked")
    public StaticAwaitStrategy(Cube cube, Map<String, Object> params) {
        this.ip = (String) params.get(IP);
        this.ports.addAll((Collection<? extends Integer>) params.get(PORTS));

        if (params.containsKey(POLLING_TIME)) {
            this.sleepPollTime = (Integer) params.get(POLLING_TIME);
        }

        if (params.containsKey(ITERATIONS)) {
            this.pollIterations = (Integer) params.get(ITERATIONS);
        }
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
