package org.arquillian.cube.impl.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.arquillian.cube.impl.util.ConfigUtil;
import org.yaml.snakeyaml.Yaml;

public class CubeConfiguration {

    private static final String DOCKER_VERSION = "serverVersion";
    private static final String DOCKER_URI = "serverUri";
    private static final String DOCKER_CONTAINERS = "dockerContainers";
    private static final String DOCKER_CONTAINERS_FILE = "dockerContainersFile";
    private static final String DOCKER_REGISTRY = "dockerRegistry";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String SHOULD_ALLOW_TO_CONNECT_TO_RUNNING_CONTAINERS = "shouldAllowToConnectToRunningContainers";

    private static final String NAME_GENERATOR = "nameGenerator";
    private static final String NAME_GENERATOR_PREFIX = "nameGeneratorPrefix";

    private String dockerServerVersion;
    private String dockerServerUri;
    private String dockerRegistry;
    private boolean shouldAllowToConnectToRunningContainers = false;
    private String[] autoStartContainers = new String[0];
    private String nameGenerator;
    private String getNameGeneratorPrefix;

    private Map<String, Object> dockerContainersContent;

    public boolean shouldAllowToConnectToRunningContainers() {
        return shouldAllowToConnectToRunningContainers;
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


    public String getNameGenerator() {
        return nameGenerator;
    }


    public String getGetNameGeneratorPrefix() {
        return getNameGeneratorPrefix;
    }


    @SuppressWarnings("unchecked")
    public static CubeConfiguration fromMap(Map<String, String> map) {

        //TODO add provider parsing here

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

        if(map.containsKey(SHOULD_ALLOW_TO_CONNECT_TO_RUNNING_CONTAINERS)) {
            cubeConfiguration.shouldAllowToConnectToRunningContainers = Boolean.parseBoolean(map.get(SHOULD_ALLOW_TO_CONNECT_TO_RUNNING_CONTAINERS));
        }

        if(map.containsKey( NAME_GENERATOR )){
            cubeConfiguration.nameGenerator = map.get( NAME_GENERATOR ).trim();
        }

        if(map.containsKey( NAME_GENERATOR_PREFIX )){
            cubeConfiguration.getNameGeneratorPrefix = map.get( NAME_GENERATOR_PREFIX ).trim();
        }

        return cubeConfiguration;
    }
}
