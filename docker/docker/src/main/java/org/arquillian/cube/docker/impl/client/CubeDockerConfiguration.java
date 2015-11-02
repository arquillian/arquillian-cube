package org.arquillian.cube.docker.impl.client;

import org.jboss.arquillian.container.spi.Container;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CubeDockerConfiguration {

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
    public static final String BOOT2DOCKER_PATH = "boot2dockerPath";
    public static final String DOCKER_MACHINE_PATH = "dockerMachinePath";
    public static final String DOCKER_MACHINE_NAME = "machineName";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String DEFINITION_FORMAT = "definitionFormat";

    private String dockerServerVersion;
    private String dockerServerUri;
    private String dockerRegistry;
    private String boot2DockerPath;
    private String dockerMachinePath;
    private String machineName;
    private String username;
    private String password;
    private String email;
    private String certPath;
    private String dockerServerIp;
    private DefinitionFormat definitionFormat = DefinitionFormat.CUBE;
    private AutoStartParser autoStartContainers = null;

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

    public String getBoot2DockerPath() {
        return boot2DockerPath;
    }

    public String getDockerMachinePath() {
        return dockerMachinePath;
    }

    public String getMachineName() {
        return machineName;
    }

    public boolean isDockerMachineName() {
        return this.getMachineName() != null;
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
    //this property is resolved in CubeConfigurator class.
    public String getDockerServerIp() {
        return dockerServerIp;
    }

    public AutoStartParser getAutoStartContainers() {
       return autoStartContainers;
    }

    void setAutoStartContainers(AutoStartParser autoStartParser) {
        this.autoStartContainers = autoStartParser;
    }

    public DefinitionFormat getDefinitionFormat() {
        return definitionFormat;
    }

    @SuppressWarnings("unchecked")
    public static CubeDockerConfiguration fromMap(Map<String, String> map) {
        CubeDockerConfiguration cubeConfiguration = new CubeDockerConfiguration();

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

        if(map.containsKey(DOCKER_MACHINE_PATH)) {
            cubeConfiguration.dockerMachinePath = map.get(DOCKER_MACHINE_PATH);
        }

        if(map.containsKey(DOCKER_MACHINE_NAME)) {
            cubeConfiguration.machineName = map.get(DOCKER_MACHINE_NAME);
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

        if(map.containsKey(DEFINITION_FORMAT)) {
            String definitionContent = map.get(DEFINITION_FORMAT);
            cubeConfiguration.definitionFormat = DefinitionFormat.valueOf(DefinitionFormat.class, definitionContent);
        }

        if (map.containsKey(DOCKER_CONTAINERS)) {
            String content = map.get(DOCKER_CONTAINERS);
            cubeConfiguration.dockerContainersContent = DockerContainerDefinitionParser.convert(content, cubeConfiguration.definitionFormat);
        }

        if (map.containsKey(DOCKER_CONTAINERS_FILE)) {
            String location = map.get(DOCKER_CONTAINERS_FILE);
            try {
                cubeConfiguration.dockerContainersContent = DockerContainerDefinitionParser.convert(URI.create(location), cubeConfiguration.definitionFormat);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if(!map.containsKey(DOCKER_CONTAINERS) && !map.containsKey(DOCKER_CONTAINERS_FILE)) {
            try {
                cubeConfiguration.dockerContainersContent = DockerContainerDefinitionParser.convertDefault(cubeConfiguration.definitionFormat);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }


        if(map.containsKey(AUTO_START_CONTAINERS)) {
            String expression = map.get(AUTO_START_CONTAINERS);
            Map<String, Object> containerDefinitions = cubeConfiguration.getDockerContainersContent();
            AutoStartParser autoStartParser = AutoStartParserFactory.create(expression, containerDefinitions);

            cubeConfiguration.autoStartContainers = autoStartParser;
        }

        return cubeConfiguration;
    }

    @Override public String toString() {
        String SEP = System.getProperty("line.separator");
        return "CubeDockerConfiguration{" + SEP +
                "dockerServerVersion='" + dockerServerVersion + '\'' + SEP +
                ", dockerServerUri='" + dockerServerUri + '\'' + SEP +
                ", dockerRegistry='" + dockerRegistry + '\'' + SEP +
                ", boot2DockerPath='" + boot2DockerPath + '\'' + SEP +
                ", dockerMachinePath='" + dockerMachinePath + '\'' + SEP +
                ", machineName='" + machineName + '\'' + SEP +
                ", username='" + username + '\'' + SEP +
                ", password='" + password + '\'' + SEP +
                ", email='" + email + '\'' + SEP +
                ", certPath='" + certPath + '\'' + SEP +
                ", dockerServerIp='" + dockerServerIp + '\'' + SEP +
                ", definitionFormat=" + definitionFormat + SEP +
                ", autoStartContainers=" + autoStartContainers + SEP +
                ", dockerContainersContent=" + dockerContainersContent + SEP +
                '}';
    }
}
