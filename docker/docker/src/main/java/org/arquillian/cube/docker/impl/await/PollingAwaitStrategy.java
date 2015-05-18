package org.arquillian.cube.docker.impl.await;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.IOUtil;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.Cube;

public class PollingAwaitStrategy implements AwaitStrategy {

    private static final Logger log = Logger.getLogger(PollingAwaitStrategy.class.getName());

    public static final String TAG = "polling";

    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final int DEFAULT_SLEEP_POLL_TIME = 500;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final String DEFAULT_POLL_TYPE = "sscommand";
    private static final String POLLING_TIME = "sleepPollingTime";
    private static final String ITERATIONS = "iterations";
    private static final String POLL_TYPE = "type";

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private int sleepPollTime = DEFAULT_SLEEP_POLL_TIME;
    private TimeUnit timeUnit = DEFAULT_TIME_UNIT;
    private String type = DEFAULT_POLL_TYPE;

    private DockerClientExecutor dockerClientExecutor;
    private Cube cube;

    public PollingAwaitStrategy(Cube cube, DockerClientExecutor dockerClientExecutor, Map<String, Object> params) {
        this.cube = cube;
        this.dockerClientExecutor = dockerClientExecutor;
        if (params.containsKey(POLLING_TIME)) {
            configurePollingTime(params);
        }

        if (params.containsKey(ITERATIONS)) {
            this.pollIterations = (Integer) params.get(ITERATIONS);
        }

        if(params.containsKey(POLL_TYPE)) {
            this.type = (String) params.get(POLL_TYPE);
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

    public int getPollIterations() {
        return pollIterations;
    }

    public int getSleepPollTime() {
        return sleepPollTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean await() {
        Binding bindings = cube.bindings();

        for (PortBinding ports : bindings.getPortBindings()) {
            log.fine(String.format("Pinging host %s and port %s with type", bindings.getIP(), ports.getBindingPort(), this.type));

            switch(this.type) {
                case "ping": {
                    if (!Ping.ping(bindings.getIP(), ports.getBindingPort(), this.pollIterations, this.sleepPollTime,
                            this.timeUnit)) {
                        return false;
                    }
                }

                break;
                case "sscommand": {
                    if(!Ping.ping(dockerClientExecutor, cube.getId(), resolveCommand("ss", ports.getExposedPort()), this.pollIterations, this.sleepPollTime, this.timeUnit)) {
                        return false;
                    }
                }
            }

        }

        return true;
    }

    private String resolveCommand(String command, int port) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("port", Integer.toString(port));
        String templateContent = IOUtil.asStringPreservingNewLines(PollingAwaitStrategy.class.getResourceAsStream(command+".sh"));
        return IOUtil.replacePlaceholders(templateContent, values);
    }
}
