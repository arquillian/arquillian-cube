package org.arquillian.cube.docker.impl.await;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.HasPortBindings.PortAddress;

public class PollingAwaitStrategy extends SleepingAwaitStrategyBase {

    private static final Logger log = Logger.getLogger(PollingAwaitStrategy.class.getName());

    public static final String TAG = "polling";

    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final String DEFAULT_POLL_TYPE = "sscommand";

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private String type = DEFAULT_POLL_TYPE;

    private DockerClientExecutor dockerClientExecutor;
    private Cube<?> cube;
    private List<Integer> ports = null;

    public PollingAwaitStrategy(Cube<?> cube, DockerClientExecutor dockerClientExecutor, Await params) {
        super(params.getSleepPollingTime());
        
        this.cube = cube;
        this.dockerClientExecutor = dockerClientExecutor;

        if (params.getIterations() != null) {
            this.pollIterations = params.getIterations();
        }

        if (params.getType() != null) {
            this.type = params.getType();
        }

        if (params.getPorts() != null) {
            this.ports = params.getPorts();
        }
    }

    public int getPollIterations() {
        return pollIterations;
    }

    public String getType() {
        return type;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    @Override
    public boolean await() {
        HasPortBindings portBindings = cube.getMetadata(HasPortBindings.class);
        if (portBindings == null) {
            log.fine("Cube does not have any ports to ping.");
            return true;
        }

        Collection<Integer> pingPorts = this.ports;
        if(ports == null) {
            pingPorts = portBindings.getBoundPorts();
        }
        for (Integer port : pingPorts) {
            switch(this.type) {
                case "ping": {
                    PortAddress mapping = portBindings.getMappedAddress(port);
                    if(mapping == null) {
                        throw new IllegalArgumentException("Can not use polling of type " + type + " on non externally bound port " + port);
                    }
                    log.fine(String.format("Pinging host %s and port %s with type", mapping.getIP(), mapping.getPort(), this.type));
                    if (!Ping.ping(mapping.getIP(), mapping.getPort(), this.pollIterations, this.getSleepTime(),
                            this.getTimeUnit())) {
                        return false;
                    }
                }

                break;
                case "sscommand": {
                    if(!Ping.ping(dockerClientExecutor, cube.getId(), resolveCommand("ss", port),
                            this.pollIterations, this.getSleepTime(), this.getTimeUnit())) {
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
