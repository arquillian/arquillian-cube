package org.arquillian.cube.impl.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.arquillian.cube.impl.util.ConfigUtil;
import org.yaml.snakeyaml.Yaml;

public class CubeConfiguration {

    private static final String DOCKER_VERSION = "serverVersion";
    public static final String DOCKER_URI = "serverUri";
    public static final String DOCKER_SERVER_IP = "dockerServerIp";
    private static final String USERNAME = "username";
    private static final String PASSWORD =  "password";
    private static final String EMAIL = "email";
    public static final String CERT_PATH = "certPath";
    private static final String DOCKER_CONTAINERS = "dockerContainers";
    private static final String DOCKER_CONTAINERS_FILE = "dockerContainersFile";
    private static final String DOCKER_REGISTRY = "dockerRegistry";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String CONNECTION_MODE = "connectionMode";
    public static final String BOOT2DOCKER_PATH = "boot2dockerPath";

    private String dockerServerVersion;
    private String dockerServerUri;
    private String dockerRegistry;
    private ConnectionMode connectionMode = ConnectionMode.STARTANDSTOP;
    private String boot2DockerPath;
    private String username;
    private String password;
    private String email;
    private String certPath;
    private String[] autoStartContainers = new String[0];
    private String dockerServerIp;

    private Map<String, Object> dockerContainersContent;

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

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

    public String[] getAutoStartContainers() {
        return autoStartContainers;
    }

    public String getBoot2DockerPath() {
        return boot2DockerPath;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getDockerServerIp() {
        return dockerServerIp;
    }

    @SuppressWarnings("unchecked")
    public static CubeConfiguration fromMap(Map<String, String> map) {
        CubeConfiguration cubeConfiguration = new CubeConfiguration();

        if(map.containsKey(DOCKER_SERVER_IP)) {
            cubeConfiguration.dockerServerIp = map.get(DOCKER_SERVER_IP);
        }

        if (map.containsKey(DOCKER_VERSION)) {
            cubeConfiguration.dockerServerVersion = map.get(DOCKER_VERSION);
        }

        if (map.containsKey(DOCKER_URI)) {
            cubeConfiguration.dockerServerUri = map.get(DOCKER_URI);
        }

        if(map.containsKey(BOOT2DOCKER_PATH)) {
            cubeConfiguration.boot2DockerPath = map.get(BOOT2DOCKER_PATH);
        }

        if(map.containsKey(USERNAME)) {
            cubeConfiguration.username = map.get(USERNAME);
        }

        if(map.containsKey(PASSWORD)) {
            cubeConfiguration.password = map.get(PASSWORD);
        }

        if(map.containsKey(EMAIL)) {
            cubeConfiguration.email = map.get(EMAIL);
        }

        if(map.containsKey(CERT_PATH)) {
            cubeConfiguration.certPath = map.get(CERT_PATH);
        }

        if (map.containsKey(DOCKER_REGISTRY)) {
            cubeConfiguration.dockerRegistry = map.get(DOCKER_REGISTRY);
        }

        if (map.containsKey(DOCKER_CONTAINERS)) {
            String content = map.get(DOCKER_CONTAINERS);
            cubeConfiguration.dockerContainersContent = ConfigUtil.applyExtendsRules((Map<String, Object>) new Yaml().load(content));
        }

        if (map.containsKey(DOCKER_CONTAINERS_FILE)) {
            String location = map.get(DOCKER_CONTAINERS_FILE);
            try {
                cubeConfiguration.dockerContainersContent = ConfigUtil.applyExtendsRules((Map<String, Object>) new Yaml().load(new FileInputStream(
                        location)));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if(map.containsKey(AUTO_START_CONTAINERS)) {
            cubeConfiguration.autoStartContainers = ConfigUtil.trim(map.get(AUTO_START_CONTAINERS).split(","));
        }

        if(map.containsKey(CONNECTION_MODE)) {
            cubeConfiguration.connectionMode = ConnectionMode.valueOf(ConnectionMode.class, map.get(CONNECTION_MODE));
        }
        return cubeConfiguration;
    }
}
