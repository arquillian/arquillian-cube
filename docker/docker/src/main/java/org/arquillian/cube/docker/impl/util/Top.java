package org.arquillian.cube.docker.impl.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Top {

    static final String DOCKER_SOCK = "docker.sock";
    static final String DOCKERINIT = ".dockerinit";
    static final String DOCKERENV = ".dockerenv";

    public Top() {
        super();
        this.dockerEnvPath = Paths.get(rootDockerFile, DOCKERENV);
        this.dockerInitPath = Paths.get(rootDockerFile, DOCKERINIT);
        this.dockerSocketFile = Paths.get(rootDockerSocket, DOCKER_SOCK);
    }

    public Top(String rootDockerFile, String rootDockerSocket) {
        super();
        this.rootDockerFile = rootDockerFile;
        this.rootDockerSocket = rootDockerSocket;
        this.dockerEnvPath = Paths.get(rootDockerFile, DOCKERENV);
        this.dockerInitPath = Paths.get(rootDockerFile, DOCKERINIT);
        this.dockerSocketFile = Paths.get(rootDockerSocket, DOCKER_SOCK);
    }

    private String rootDockerFile = "/";
    private String rootDockerSocket = "/var/run/";

    private final Path dockerEnvPath;
    private final Path dockerInitPath;
    private final Path dockerSocketFile;

    /**
     * Checks if current code is being executed inside Docker or not.
     * @return True if code is being executed inside Docker, false otherwise.
     */
    public boolean isSpinning() {
        return Files.exists(dockerEnvPath) && Files.exists(dockerInitPath) && Files.exists(dockerSocketFile);
    }
}
