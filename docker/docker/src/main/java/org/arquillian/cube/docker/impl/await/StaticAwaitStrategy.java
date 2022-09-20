package org.arquillian.cube.docker.impl.await;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.spi.Cube;

public class StaticAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "static";

    private static final int DEFAULT_POLL_ITERATIONS = 10;

    private int pollIterations = DEFAULT_POLL_ITERATIONS;

    private String ip;
    private List<Integer> ports = new ArrayList<Integer>();

    public StaticAwaitStrategy(final Cube<?> cube, final DockerClientExecutor dockerClientExecutor, final Await params) {
        super(params.getSleepPollingTime());

        this.ip = (dockerClientExecutor.isDockerInsideDockerResolution()
            ? dockerClientExecutor.getDockerServerIp() : dockerClientExecutor.getDockerUri().getHost());

        this.ports.addAll(params.getPorts());

        Logger.getLogger(StaticAwaitStrategy.class.getName()).log(Level.INFO, String.format("Static await strategy host for %s is: %s:%s"
            , cube.getId(), this.ip, this.ports));

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
