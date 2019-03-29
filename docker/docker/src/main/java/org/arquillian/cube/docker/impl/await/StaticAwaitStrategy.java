package org.arquillian.cube.docker.impl.await;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.BindingUtil;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.spi.Cube;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StaticAwaitStrategy extends SleepingAwaitStrategyBase {

    public static final String TAG = "static";

    private static final int DEFAULT_POLL_ITERATIONS = 10;

    private int pollIterations = DEFAULT_POLL_ITERATIONS;

    private String ip;
    private List<Integer> ports = new ArrayList<Integer>();

    public StaticAwaitStrategy(final Cube<?> cube, final DockerClientExecutor dockerClientExecutor, final Await params, final boolean dind) {
        super(params.getSleepPollingTime());

        this.ip = (null != params.getIp() ? params.getIp() : "localhost");

        if (dind) {
            this.ip = BindingUtil.getContainerIp(dockerClientExecutor, cube.getId());
        }

        this.ports.addAll(params.getPorts());

        Logger.getLogger(StaticAwaitStrategy.class.getName()).log(Level.INFO, String.format("Static await strategy for dind:%s '%s' is: %s:%s"
            , dind, cube.getId(), this.ip, this.ports));

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
