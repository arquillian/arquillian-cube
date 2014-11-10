package org.arquillian.cube.impl.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class CubeConfiguration {

    private static final String DOCKER_VERSION = "serverVersion";
    private static final String DOCKER_URI = "serverUri";
    private static final String DOCKER_CONTAINERS = "dockerContainers";
    private static final String DOCKER_CONTAINERS_FILE = "dockerContainersFile";
    private static final String DOCKER_REGISTRY = "dockerRegistry";

    private String dockerServerVersion;
    private String dockerServerUri;
    private String dockerRegistry;

    private Map<String, Object> dockerContainersContent;

    public String getDockerServerUri() {
        return dockerServerUri;
    }

    public String getDockerServerVersion() {
        return dockerServerVersion;
    }

    public Map<String, Object> getDockerContainersContent() {
        return dockerContainersContent;
    }

    public String getDockerRegistry() {
        return dockerRegistry;
    }

    @SuppressWarnings("unchecked")
    public static CubeConfiguration fromMap(Map<String, String> map) {
        CubeConfiguration cubeConfiguration = new CubeConfiguration();

        if (map.containsKey(DOCKER_VERSION)) {
            cubeConfiguration.dockerServerVersion = map.get(DOCKER_VERSION);
        }

        if (map.containsKey(DOCKER_URI)) {
            cubeConfiguration.dockerServerUri = map.get(DOCKER_URI);
        }

        if (map.containsKey(DOCKER_REGISTRY)) {
            cubeConfiguration.dockerRegistry = map.get(DOCKER_REGISTRY);
        }

        if (map.containsKey(DOCKER_CONTAINERS)) {
            String content = map.get(DOCKER_CONTAINERS);
            cubeConfiguration.dockerContainersContent = (Map<String, Object>) new Yaml().load(content);
        }

        if (map.containsKey(DOCKER_CONTAINERS_FILE)) {
            String location = map.get(DOCKER_CONTAINERS_FILE);
            try {
                cubeConfiguration.dockerContainersContent = (Map<String, Object>) new Yaml().load(new FileInputStream(
                        location));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return cubeConfiguration;
    }
}
