package org.arquillian.cube.docker.impl.util;

import java.io.File;

public class Top {

    static final String DOCKER_SOCK = "docker.sock";
    static final String DOCKERINIT  = ".dockerinit";
    static final String DOCKERENV   = ".dockerenv";

    public Top() {
        super();
        this.dockerEnvPath = new File(rootDockerFile, DOCKERENV);
        this.dockerInitPath = new File(rootDockerFile, DOCKERINIT);
        this.dockerSocketFile = new File(rootDockerSocket, DOCKER_SOCK);
    }
    public Top(String rootDockerFile, String rootDockerSocket) {
        super();
        this.rootDockerFile = rootDockerFile;
        this.rootDockerSocket = rootDockerSocket;
        this.dockerEnvPath = new File(rootDockerFile, DOCKERENV);
        this.dockerInitPath = new File(rootDockerFile, DOCKERINIT);
        this.dockerSocketFile = new File(rootDockerSocket, DOCKER_SOCK);
    }


    private String rootDockerFile   = "/";
    private String rootDockerSocket = "/var/run/";

    private final File dockerEnvPath;
    private final File dockerInitPath;
    private final File dockerSocketFile;

    /**
     * Checks if current code is being executed inside Docker or not.
     * @return True if code is being executed inside Docker, false otherwise.
     */
    public boolean isSpinning() {
        return dockerEnvPath.exists() && dockerInitPath.exists() && dockerSocketFile.exists();
    }
}
