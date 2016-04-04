package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.arquillian.cube.docker.impl.client.config.CubeContainers;
import org.arquillian.cube.docker.impl.model.DockerMachineDistro;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;

public class CubeDockerConfiguration {

    private static final String DOCKER_VERSION = "serverVersion";
    public static final String DOCKER_URI = "serverUri";
    public static final String DOCKER_SERVER_IP = "dockerServerIp";
    private static final String USERNAME = "username";
    private static final String PASSWORD =  "password";
    private static final String EMAIL = "email";
    public static final String CERT_PATH = "certPath";
    public static final String TLS_VERIFY = "tlsVerify";
    private static final String DOCKER_CONTAINERS = "dockerContainers";
    private static final String DOCKER_CONTAINERS_FILE = "dockerContainersFile";
    private static final String DOCKER_CONTAINERS_FILES = "dockerContainersFiles";
    private static final String DOCKER_REGISTRY = "dockerRegistry";
    public static final String BOOT2DOCKER_PATH = "boot2dockerPath";
    public static final String DOCKER_MACHINE_PATH = "dockerMachinePath";
    public static final String DOCKER_MACHINE_NAME = "machineName";
    public static final String DOCKER_MACHINE_DRIVER = "machineDriver";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String DEFINITION_FORMAT = "definitionFormat";
    static final String DIND_RESOLUTION = "dockerInsideDockerResolution";
    private static final String CUBE_ENVIRONMENT = "cube.environment";
    public static final String DOCKER_MACHINE_CUSTOM_PATH = "dockerMachineCustomPath";
    public static final String DOCKER_MACHINE_ARQUILLIAN_PATH = "~/.arquillian/machine";
    public static final String CUBE_SPECIFIC_PROPERTIES = "cubeSpecificProperties";
    public static final String CLEAN = "clean";

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
    private boolean tlsVerify;
    private String dockerServerIp;
    private DefinitionFormat definitionFormat = DefinitionFormat.CUBE;
    private boolean dockerInsideDockerResolution = true;
    private boolean clean = false;
    private AutoStartParser autoStartContainers = null;

    private CubeContainers dockerContainersContent;

    public String getDockerServerUri() {
        return dockerServerUri;
    }

    public String getDockerServerVersion() {
        return dockerServerVersion;
    }

    public CubeContainers getDockerContainersContent() {
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

    public boolean getTlsVerify() {
        return tlsVerify;
    }

    //this property is resolved in CubeConfigurator class.
    public String getDockerServerIp() {
        return dockerServerIp;
    }

    public AutoStartParser getAutoStartContainers() {
       return autoStartContainers;
    }

    public boolean isClean() {
        return clean;
    }

    void setAutoStartContainers(AutoStartParser autoStartParser) {
        this.autoStartContainers = autoStartParser;
    }

    public DefinitionFormat getDefinitionFormat() {
        return definitionFormat;
    }

    public boolean isDockerInsideDockerResolution() {
        return dockerInsideDockerResolution;
    }

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

        if (map.containsKey(DIND_RESOLUTION)) {
            cubeConfiguration.dockerInsideDockerResolution = Boolean.parseBoolean(map.get(DIND_RESOLUTION));
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

        if (map.containsKey(TLS_VERIFY)) {
            cubeConfiguration.tlsVerify = Boolean.parseBoolean(map.get(TLS_VERIFY));
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
            final String location = map.get(DOCKER_CONTAINERS_FILE);
            final List<URI> resolveUri = new ArrayList<>();
            try {
                final URI uri = URI.create(location);
                resolveUri.add(uri);

                if (System.getProperty(CUBE_ENVIRONMENT) != null) {
                    final String resolveFilename = resolveFilename(uri);
                    final String environmentUri = uri.toString().replace(resolveFilename, resolveFilename + "." + System.getProperty(CUBE_ENVIRONMENT));
                    resolveUri.add(URI.create(environmentUri));
                }

                cubeConfiguration.dockerContainersContent = DockerContainerDefinitionParser.convert(cubeConfiguration.definitionFormat, resolveUri.toArray(new URI[resolveUri.size()]));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (map.containsKey(DOCKER_CONTAINERS_FILES)) {

            String locations = map.get(DOCKER_CONTAINERS_FILES);
            List<URI> realLocations = getUris(locations);
            try {
                cubeConfiguration.dockerContainersContent = DockerContainerDefinitionParser.convert(cubeConfiguration.definitionFormat, realLocations.toArray(new URI[realLocations.size()]));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (!map.containsKey(DOCKER_CONTAINERS) && !map.containsKey(DOCKER_CONTAINERS_FILE) && !map.containsKey(DOCKER_CONTAINERS_FILES)) {
            try {
                cubeConfiguration.dockerContainersContent = DockerContainerDefinitionParser.convertDefault(cubeConfiguration.definitionFormat);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (map.containsKey(CUBE_SPECIFIC_PROPERTIES)) {
            String content = map.get(CUBE_SPECIFIC_PROPERTIES);
            final CubeContainers overrideInformation = DockerContainerDefinitionParser.convert(content, DefinitionFormat.CUBE);
            cubeConfiguration.dockerContainersContent.overrideCubeProperties(overrideInformation);
        }

        if (map.containsKey(AUTO_START_CONTAINERS)) {
            String expression = map.get(AUTO_START_CONTAINERS);
            CubeContainers containerDefinitions = cubeConfiguration.getDockerContainersContent();
            AutoStartParser autoStartParser = AutoStartParserFactory.create(expression, containerDefinitions);

            cubeConfiguration.autoStartContainers = autoStartParser;
        }

        if (map.containsKey(CLEAN)) {
            cubeConfiguration.clean = Boolean.parseBoolean(map.get(CLEAN));
        }
        
        return cubeConfiguration;
    }

    private static String resolveFilename(URI uri) {
        if (uri.getScheme() == null || "file".equals(uri.getScheme())) {
            //it is a local path
            final String fullPath = uri.toString();
            final int lastSeparatorChar = fullPath.lastIndexOf(File.separatorChar);
            if (lastSeparatorChar > -1) {
                return fullPath.substring(lastSeparatorChar + 1, fullPath.lastIndexOf('.'));
            } else {
                return fullPath.substring(0, fullPath.lastIndexOf('.'));
            }
        } else {
            //means it is a remote uri (http, ftp, ...
            final String fullPath = uri.toString();
            final int lastSeparatorChar = fullPath.lastIndexOf(File.separatorChar);
            return fullPath.substring(lastSeparatorChar + 1, fullPath.lastIndexOf('.'));
        }
    }

    private static List<URI> getUris(String locations) {
        // Transform comma-separated values to an array of URIs
        List<URI> realLocations = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(locations, ",");
        while (tokenizer.hasMoreTokens()) {
            realLocations.add(URI.create(tokenizer.nextToken().trim()));
        }
        return realLocations;
    }

    @Override public String toString() {
        String SEP = System.getProperty("line.separator");
        StringBuilder content = new StringBuilder();

        content.append("CubeDockerConfiguration: ").append(SEP);
        if (dockerServerVersion != null) {
            content.append("  ").append(DOCKER_VERSION).append(" = ").append(dockerServerVersion).append(SEP);
        }
        if (dockerServerUri != null) {
            content.append("  ").append(DOCKER_URI).append(" = ").append(dockerServerUri).append(SEP);
        }
        if (dockerRegistry != null) {
            content.append("  ").append(DOCKER_REGISTRY).append(" = ").append(dockerRegistry).append(SEP);
        }
        if (boot2DockerPath != null) {
            content.append("  ").append(BOOT2DOCKER_PATH).append(" = ").append(boot2DockerPath).append(SEP);
        }
        if (dockerMachinePath != null) {
            content.append("  ").append(DOCKER_MACHINE_PATH).append(" = ").append(dockerMachinePath).append(SEP);
        }
        if (machineName != null) {
            content.append("  ").append(DOCKER_MACHINE_NAME).append(" = ").append(machineName).append(SEP);
        }
        if (username != null) {
            content.append("  ").append(USERNAME).append(" = ").append(username).append(SEP);
        }
        if (password != null) {
            content.append("  ").append(PASSWORD).append(" = ").append(password).append(SEP);
        }
        if (email != null) {
            content.append("  ").append(EMAIL).append(" = ").append(email).append(SEP);
        }
        if (certPath != null) {
            content.append("  ").append(CERT_PATH).append(" = ").append(certPath).append(SEP);
        }

        content.append("  ").append(TLS_VERIFY).append(" = ").append(tlsVerify).append(SEP);

        if (dockerServerIp != null) {
            content.append("  ").append(DOCKER_SERVER_IP).append(" = ").append(dockerServerIp).append(SEP);
        }
        if (definitionFormat != null) {
            content.append("  ").append(DEFINITION_FORMAT).append(" = ").append(definitionFormat).append(SEP);
        }
        if (autoStartContainers != null) {
            content.append("  ").append(AUTO_START_CONTAINERS).append(" = ").append(autoStartContainers).append(SEP);
        }
        if (clean) {
            content.append("  ").append(CLEAN).append(" = ").append(clean).append(SEP);
        }
        if (dockerContainersContent != null) {
            String output = ConfigUtil.dump(dockerContainersContent);
            content.append("  ").append(DOCKER_CONTAINERS).append(" = ").append(output).append(SEP);
        }

        return content.toString();
    }

    public static String resolveUrl(String machineVersion) {
        return "https://github.com/docker/machine/releases/download/" + machineVersion + "/" + DockerMachineDistro.resolveDistro();
    }

    public static String resolveMachinePath(String machineCustomPath, String machineVersion) {
        if (StringUtils.isBlank(machineCustomPath)) {
            machineCustomPath = DOCKER_MACHINE_ARQUILLIAN_PATH;
        }
        return HomeResolverUtil.resolveHomeDirectoryChar(machineCustomPath + "/" + machineVersion + "/" + DockerMachine.DOCKER_MACHINE_EXEC);
    }

}
