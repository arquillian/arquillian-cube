package org.arquillian.cube.impl.await;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.arquillian.cube.impl.util.Ping;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.Cube;

public class PollingAwaitStrategy implements AwaitStrategy {

    private static final Logger log = Logger.getLogger(PollingAwaitStrategy.class.getName());

    public static final String TAG = "polling";

    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final int DEFAULT_SLEEP_POLL_TIME = 500;
    private static final String POLLING_TIME = "sleepPollingTime";
    private static final String ITERATIONS = "iterations";

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private int sleepPollTime = DEFAULT_SLEEP_POLL_TIME;

    private Cube cube;

    public PollingAwaitStrategy(Cube cube, Map<String, Object> params) {
        this.cube = cube;
        if (params.containsKey(POLLING_TIME)) {
            this.sleepPollTime = (Integer) params.get(POLLING_TIME);
        }

        if (params.containsKey(ITERATIONS)) {
            this.pollIterations = (Integer) params.get(ITERATIONS);
        }
    }

    public int getPollIterations() {
        return pollIterations;
    }

    public int getSleepPollTime() {
        return sleepPollTime;
    }

    @Override
    public boolean await() {
        Binding bindings = cube.bindings();

        for (PortBinding ports : bindings.getPortBindings()) {
            log.fine(String.format("Pinging host (gateway) %s and port %s", bindings.getIP(), ports.getBindingPort()));
            if (!Ping.ping(bindings.getIP(), ports.getBindingPort(), this.pollIterations, this.sleepPollTime,
                    TimeUnit.MILLISECONDS)) {
                return false;
            }
        }

        return true;
    }
}
