package org.arquillian.cube.docker.impl.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeOutput;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.HasPortBindings.PortAddress;

public class PollingAwaitStrategy extends SleepingAwaitStrategyBase {

    private static final Logger log = Logger.getLogger(PollingAwaitStrategy.class.getName());
    private static final String MESSAGE = "Service is Up";
    private static final String WAIT_FOR_IT_SCRIPT = "wait-for-it.sh";

    public static final String TAG = "polling";

    private static final int DEFAULT_POLL_ITERATIONS = 40;
    private static final String DEFAULT_POLL_TYPE = "sscommand";
    public static final String CONTAINER_DIRECTORY = "/tmp";

    private int pollIterations = DEFAULT_POLL_ITERATIONS;
    private String type = DEFAULT_POLL_TYPE;

    private DockerClientExecutor dockerClientExecutor;
    private Cube<?> cube;
    private List<Integer> ports = null;

    // To avoid having to copy the script for all ports, state is saved.
    private boolean alreadyCopiedWaitForIt = false;

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

        if (params.getPorts() != null && params.getPorts().size() > 0) {
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
        if (ports == null) {
            pingPorts = portBindings.getBoundPorts();
        }
        for (Integer port : pingPorts) {
            switch (this.type) {
                case "ping": {
                    PortAddress mapping = portBindings.getMappedAddress(port);
                    if (mapping == null) {
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
                    try {
                        if (!Ping.ping(dockerClientExecutor, cube.getId(), resolveCommand("ss", port),
                                this.pollIterations, this.getSleepTime(), this.getTimeUnit())) {
                            return false;
                        }
                    } catch (UnsupportedOperationException e) {
                        // In case of not having ss command installed on container, it automatically fall back to waitforit approach
                        try {
                            if (!executeWaitForIt(portBindings.getInternalIP(), port)) {
                                return false;
                            }
                        } catch (UnsupportedOperationException ex) {
                            PortAddress mapping = portBindings.getMappedAddress(port);
                            if (mapping == null) {
                                throw new IllegalArgumentException("Can not use polling of type " + type + " on non externally bound port " + port);
                            }
                            log.fine(String.format("Pinging host %s and port %s with type", mapping.getIP(), mapping.getPort(), this.type));
                            if (!Ping.ping(mapping.getIP(), mapping.getPort(), this.pollIterations, this.getSleepTime(),
                                    this.getTimeUnit())) {
                                return false;
                            }
                        }
                    }
                }
                break;
                case "waitforit": {
                    if (!executeWaitForIt(portBindings.getInternalIP(), port)) {
                        return false;
                    }
                }
            }

        }

        return true;
    }

    private boolean executeWaitForIt(String containerIp, int port) {

        // We copy our wait-for-it-sh.sh file form classpath
        // Our wait-for-it-sh.sh script also works with busybox/alpine which is not true with the official one.

        try {

            if (!alreadyCopiedWaitForIt) {
                final Path waitForItLocation = copyWaitForItScriptToTempDir();
                // Then copy this into the container
                dockerClientExecutor.copyStreamToContainer(cube.getId(), waitForItLocation.toFile(), new File(CONTAINER_DIRECTORY));
                alreadyCopiedWaitForIt = true;
            }

            String command = resolveWaitForItCommand(containerIp, port);
            final String[] commands = {"sh", "-c", command};
            CubeOutput result = dockerClientExecutor.execStart(cube.getId(), commands);

            if (result.getError() != null && result.getError().contains("can't execute")) {
                throw new UnsupportedOperationException(result.getError());
            }

            return result.getStandard() != null && result.getStandard().trim().contains(MESSAGE);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private Path copyWaitForItScriptToTempDir() throws IOException {
        final Path arquilliancube = Files.createTempDirectory("arquilliancube");
        final Path waitForItLocation = arquilliancube.resolve(Paths.get(WAIT_FOR_IT_SCRIPT));
        Files.copy(PollingAwaitStrategy.class.getResourceAsStream("/org/arquillian/cube/docker/impl/await/wait-for-it.sh"), waitForItLocation);
        Files.setPosixFilePermissions(waitForItLocation, getScriptPermissions());

        return waitForItLocation;
    }

    private Set<PosixFilePermission> getScriptPermissions() {

        final PosixFilePermission ownerExecute = PosixFilePermission.OWNER_EXECUTE;
        final PosixFilePermission groupExecute = PosixFilePermission.GROUP_EXECUTE;
        final PosixFilePermission othersExecute = PosixFilePermission.OTHERS_EXECUTE;
        final PosixFilePermission ownerRead = PosixFilePermission.OWNER_READ;
        final PosixFilePermission groupRead = PosixFilePermission.GROUP_READ;
        final PosixFilePermission othersRead = PosixFilePermission.OTHERS_READ;

        final Set<PosixFilePermission> perms = new HashSet<>();
        perms.addAll(Arrays.asList(
                ownerExecute, ownerRead,
                groupExecute, groupRead,
                othersExecute, othersRead
                )
        );

        return perms;
    }

    private String resolveWaitForItCommand(String containerIp, int port) {
        return String.format("%s/%s %s:%s -s -- echo %s", CONTAINER_DIRECTORY, WAIT_FOR_IT_SCRIPT, containerIp, port, MESSAGE);
    }

    private String resolveCommand(String command, int port) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("port", Integer.toString(port));
        String templateContent = IOUtil.asStringPreservingNewLines(PollingAwaitStrategy.class.getResourceAsStream(command + ".sh"));
        return IOUtil.replacePlaceholders(templateContent, values);
    }
}
