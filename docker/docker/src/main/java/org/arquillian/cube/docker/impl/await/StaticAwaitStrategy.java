package org.arquillian.cube.docker.impl.await;

import java.util.ArrayList;
import java.util.List;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.spi.Cube;

public class StaticAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "static";

    private static final int DEFAULT_POLL_ITERATIONS = 10;

    private int pollIterations = DEFAULT_POLL_ITERATIONS;

    private String ip;
    private List<Integer> ports = new ArrayList<Integer>();

    public StaticAwaitStrategy(Cube<?> cube, Await params) {
        super(params.getSleepPollingTime());
        
        this.ip = params.getIp();
        this.ports.addAll(params.getPorts());

        if (params.getIterations() != null) {
            this.pollIterations = params.getIterations();
        }
    }

    @Override
    public boolean await() {

        for (Integer port : this.ports) {
            if (!Ping.ping(this.ip, port, this.pollIterations, this.getSleepTime(), this.getTimeUnit())) {
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

}
