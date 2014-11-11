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

    private String ip;
    private List<Integer> ports = new ArrayList<Integer>();

    @SuppressWarnings("unchecked")
    public StaticAwaitStrategy(Cube cube, Map<String, Object> params) {
        this.ip = (String) params.get(IP);
        this.ports.addAll((Collection<? extends Integer>) params.get(PORTS));
    }

    @Override
    public boolean await() {

        for (Integer port : this.ports) {
            if(!Ping.ping(this.ip, port, DEFAULT_POLL_ITERATIONS, DEFAULT_SLEEP_POLL_TIME, TimeUnit.MILLISECONDS )) {
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

}
