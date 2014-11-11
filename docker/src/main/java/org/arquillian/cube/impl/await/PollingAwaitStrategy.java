package org.arquillian.cube.impl.await;

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

    private Cube cube;

    public PollingAwaitStrategy(Cube cube) {
        this.cube = cube;
    }

    @Override
    public boolean await() {
        Binding bindings = cube.bindings();

        for (PortBinding ports : bindings.getPortBindings()) {
            log.fine(String.format("Pinging host (gateway) %s and port %s", bindings.getIP(), ports.getBindingPort()));
            if(!Ping.ping(bindings.getIP(), ports.getBindingPort(), DEFAULT_POLL_ITERATIONS, DEFAULT_SLEEP_POLL_TIME,
                    TimeUnit.MILLISECONDS)) {
                return false;
            }
        }

        return true;
    }
}
