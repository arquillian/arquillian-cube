package org.arquillian.cube.docker.impl.await;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.HasPortBindings.PortAddress;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.task.net.DownloadTool;

public class PollingAwaitStrategy extends SleepingAwaitStrategyBase {

    private static final Logger log = Logger.getLogger(PollingAwaitStrategy.class.getName());
    private static final String MESSAGE = "Service is Up";
    private static final String WAIT_FOR_IT_SCRIPT_DOWNLOAD_LOCATION = "https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh";
    private static final String WAIT_FOR_IT_SCRIPT_DIRECTORY = HomeResolverUtil.resolveHomeDirectoryChar("~/.arquillian");
    private static final String WAIT_FOR_IT_SCRIPT = WAIT_FOR_IT_SCRIPT_DIRECTORY + "/wait-for-it.sh";

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
                break;
                case "waitforit": {
                    if (! executeWaitForIt(portBindings.getInternalIP(), port)) {
                        return false;
                    }
                }
            }

        }

        return true;
    }

    private boolean executeWaitForIt(String containerIp, int port) {
        if (! new File(WAIT_FOR_IT_SCRIPT_DIRECTORY).exists()) {
            if (! new File(WAIT_FOR_IT_SCRIPT_DIRECTORY).mkdirs()) {
                throw new IllegalArgumentException("Couldn't create the Arquillian directory at ~/.arquillian");
            }
        }

        // Check if in (~/.arquillian/wait-for-it.sh) is the file
        if (! new File(WAIT_FOR_IT_SCRIPT).exists()) {
            // If not then download the script
            Spacelift.task(DownloadTool.class)
                    .from(WAIT_FOR_IT_SCRIPT_DOWNLOAD_LOCATION)
                    .to(WAIT_FOR_IT_SCRIPT)
                    .execute()
                    .await();
        }
        // Then copy this into the container
        dockerClientExecutor.copyStreamToContainer(cube.getId(), new File(WAIT_FOR_IT_SCRIPT));
        // Fix permissions
        dockerClientExecutor.execStart(cube.getId(), "sh", "-c", "chmod 744 wait-for-it.sh");

        String command = resolveWaitForItCommand(containerIp, port);
        final String[] commands = {"sh", "-c", command};
        String result = dockerClientExecutor.execStart(cube.getId(), commands);
        return result != null && result.trim().contains(MESSAGE);
    }

    private String resolveWaitForItCommand(String containerIp, int port) {
        return String.format("/wait-for-it.sh %s:%s -s -- echo %s", containerIp, port, MESSAGE);
    }

    private String resolveCommand(String command, int port) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("port", Integer.toString(port));
        String templateContent = IOUtil.asStringPreservingNewLines(PollingAwaitStrategy.class.getResourceAsStream(command+".sh"));
        return IOUtil.replacePlaceholders(templateContent, values);
    }
}
