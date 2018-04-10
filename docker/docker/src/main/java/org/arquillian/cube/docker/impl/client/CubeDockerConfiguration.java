package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.lang.StringUtils;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.model.DockerMachineDistro;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.AutoStartParser;
import org.jboss.arquillian.core.api.Injector;

public class CubeDockerConfiguration {

    public static final String DOCKER_URI = "serverUri";
    public static final String DOCKER_SERVER_IP = "dockerServerIp";
    public static final String CERT_PATH = "certPath";
    public static final String TLS_VERIFY = "tlsVerify";
    public static final String DOCKER_CONTAINERS = "dockerContainers";
    public static final String BOOT2DOCKER_PATH = "boot2dockerPath";
    public static final String DOCKER_MACHINE_PATH = "dockerMachinePath";
    public static final String DOCKER_MACHINE_NAME = "machineName";
    public static final String DOCKER_MACHINE_DRIVER = "machineDriver";
    public static final String AUTO_START_ORDER = "autoStartOrder";
    public static final String DOCKER_MACHINE_CUSTOM_PATH = "dockerMachineCustomPath";
    public static final String DOCKER_MACHINE_ARQUILLIAN_PATH = "~/.arquillian/machine";
    public static final String CUBE_SPECIFIC_PROPERTIES = "cubeSpecificProperties";
    public static final String CLEAN = "clean";
    public static final String REMOVE_VOLUMES = "removeVolumes";
    public static final String CLEAN_BUILD_IMAGE = "cleanBuildImage";
    static final String DIND_RESOLUTION = "dockerInsideDockerResolution";
    private static final String DOCKER_VERSION = "serverVersion";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String EMAIL = "email";
    private static final String DOCKER_CONTAINERS_FILE = "dockerContainersFile";
    private static final String DOCKER_CONTAINERS_FILES = "dockerContainersFiles";
    private static final String DOCKER_CONTAINERS_RESOURCE = "dockerContainersResource";
    private static final String IGNORE_CONTAINERS_DEFINITION = "ignoreContainersDefinition";
    private static final String DOCKER_REGISTRY = "dockerRegistry";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String DEFINITION_FORMAT = "definitionFormat";
    private static final String CUBE_ENVIRONMENT = "cube.environment";
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
    private DefinitionFormat definitionFormat = DefinitionFormat.COMPOSE;
    private boolean dockerInsideDockerResolution = true;
    private boolean clean = false;
    private boolean removeVolumes = true;
    private boolean cleanBuildImage = true;
    private boolean ignoreContainersDefinition = false;

    private AutoStartParser autoStartContainers = null;
    private DockerAutoStartOrder dockerAutoStartOrder = null;

    private DockerCompositions dockerContainersContent;

    public static CubeDockerConfiguration fromMap(Map<String, String> map, Injector injector) {
        CubeDockerConfiguration cubeConfiguration = new CubeDockerConfiguration();

        if (map.containsKey(DOCKER_SERVER_IP)) {
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

        if (map.containsKey(BOOT2DOCKER_PATH)) {
            cubeConfiguration.boot2DockerPath = map.get(BOOT2DOCKER_PATH);
        }

        if (map.containsKey(DOCKER_MACHINE_PATH)) {
            cubeConfiguration.dockerMachinePath = map.get(DOCKER_MACHINE_PATH);
        }

        if (map.containsKey(DOCKER_MACHINE_NAME)) {
            cubeConfiguration.machineName = map.get(DOCKER_MACHINE_NAME);
        }

        if (map.containsKey(USERNAME)) {
            cubeConfiguration.username = map.get(USERNAME);
        }

        if (map.containsKey(PASSWORD)) {
            cubeConfiguration.password = map.get(PASSWORD);
        }

        if (map.containsKey(EMAIL)) {
            cubeConfiguration.email = map.get(EMAIL);
        }

        if (map.containsKey(CERT_PATH)) {
            cubeConfiguration.certPath = map.get(CERT_PATH);
        }

        if (map.containsKey(TLS_VERIFY)) {
            cubeConfiguration.tlsVerify = Boolean.parseBoolean(map.get(TLS_VERIFY));
        }

        if (map.containsKey(DOCKER_REGISTRY)) {
            cubeConfiguration.dockerRegistry = map.get(DOCKER_REGISTRY);
        }

        if (map.containsKey(IGNORE_CONTAINERS_DEFINITION)) {
            cubeConfiguration.ignoreContainersDefinition = Boolean.parseBoolean(map.get(IGNORE_CONTAINERS_DEFINITION));
        }

        if (map.containsKey(DEFINITION_FORMAT)) {
            String definitionContent = map.get(DEFINITION_FORMAT);
            cubeConfiguration.definitionFormat = DefinitionFormat.valueOf(DefinitionFormat.class, definitionContent);
        }

        if (map.containsKey(DOCKER_CONTAINERS) && !cubeConfiguration.ignoreContainersDefinition) {
            String content = map.get(DOCKER_CONTAINERS);
            cubeConfiguration.dockerContainersContent =
                DockerContainerDefinitionParser.convert(content, cubeConfiguration.definitionFormat);
        }

        if (map.containsKey(DOCKER_CONTAINERS_FILE) && !cubeConfiguration.ignoreContainersDefinition) {
            final String location = map.get(DOCKER_CONTAINERS_FILE);
            final List<URI> resolveUri = new ArrayList<>();
            try {
                final URI uri = URI.create(location);
                resolveUri.add(uri);

                if (System.getProperty(CUBE_ENVIRONMENT) != null) {
                    final String resolveFilename = resolveFilename(uri);
                    final String environmentUri = uri.toString()
                        .replace(resolveFilename, resolveFilename + "." + System.getProperty(CUBE_ENVIRONMENT));
                    resolveUri.add(URI.create(environmentUri));
                }

                cubeConfiguration.dockerContainersContent =
                    DockerContainerDefinitionParser.convert(cubeConfiguration.definitionFormat,
                        resolveUri.toArray(new URI[resolveUri.size()]));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (map.containsKey(DOCKER_CONTAINERS_FILES) && !cubeConfiguration.ignoreContainersDefinition) {

            String locations = map.get(DOCKER_CONTAINERS_FILES);
            List<URI> realLocations = getUris(locations);
            try {
                cubeConfiguration.dockerContainersContent =
                    DockerContainerDefinitionParser.convert(cubeConfiguration.definitionFormat,
                        realLocations.toArray(new URI[realLocations.size()]));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (map.containsKey(DOCKER_CONTAINERS_RESOURCE) && !cubeConfiguration.ignoreContainersDefinition) {
            String resource = map.get(DOCKER_CONTAINERS_RESOURCE);
            InputStream stream = CubeDockerConfiguration.class.getClassLoader().getResourceAsStream(resource);
            if (stream != null) {
                cubeConfiguration.dockerContainersContent =
                    DockerContainerDefinitionParser.convert(
                            IOUtil.asStringPreservingNewLines(stream),
                            cubeConfiguration.definitionFormat);
            } else {
                throw new IllegalArgumentException("Resource " + resource + " not found in classpath");
            }
        }

        if (!map.containsKey(DOCKER_CONTAINERS) && !map.containsKey(DOCKER_CONTAINERS_FILE)
            && !map.containsKey(DOCKER_CONTAINERS_FILES) && !map.containsKey(DOCKER_CONTAINERS_RESOURCE)
            && !cubeConfiguration.ignoreContainersDefinition) {
            try {
                cubeConfiguration.dockerContainersContent =
                    DockerContainerDefinitionParser.convertDefault(cubeConfiguration.definitionFormat);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (map.containsKey(CUBE_SPECIFIC_PROPERTIES)) {
            String content = map.get(CUBE_SPECIFIC_PROPERTIES);
            final DockerCompositions overrideInformation =
                DockerContainerDefinitionParser.convert(content, DefinitionFormat.CUBE);
            cubeConfiguration.dockerContainersContent.overrideCubeProperties(overrideInformation);
        }

        if (map.containsKey(AUTO_START_CONTAINERS)) {
            String expression = map.get(AUTO_START_CONTAINERS);
            DockerCompositions containerDefinitions = cubeConfiguration.getDockerContainersContent();
            AutoStartParser autoStartParser = AutoStartParserFactory.create(expression, containerDefinitions, injector);

            cubeConfiguration.autoStartContainers = autoStartParser;
        }

        if (map.containsKey(CLEAN)) {
            cubeConfiguration.clean = Boolean.parseBoolean(map.get(CLEAN));
        }

        if (map.containsKey(AUTO_START_ORDER)) {
            cubeConfiguration.dockerAutoStartOrder =
                AutoStartOrderFactory.createDockerAutoStartOrder(map.get(AUTO_START_ORDER));
        } else {
            cubeConfiguration.dockerAutoStartOrder = AutoStartOrderFactory.createDefaultDockerAutoStartOrder();
        }

        if (map.containsKey(REMOVE_VOLUMES)) {
            cubeConfiguration.removeVolumes = Boolean.parseBoolean(map.get(REMOVE_VOLUMES));
        }

        if (map.containsKey(CLEAN_BUILD_IMAGE)) {
            cubeConfiguration.cleanBuildImage = Boolean.parseBoolean(map.get(CLEAN_BUILD_IMAGE));
        }

        for (CubeContainer container : cubeConfiguration.dockerContainersContent.getContainers().values()) {
            if (container.getRemoveVolumes() == null) {
                container.setRemoveVolumes(cubeConfiguration.isRemoveVolumes());
            }
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

    public static String resolveUrl(String machineVersion) {
        return "https://github.com/docker/machine/releases/download/"
            + machineVersion
            + "/"
            + DockerMachineDistro.resolveDistro();
    }

    public static File resolveMachinePath(String machineCustomPath, String machineVersion) {
        if (StringUtils.isBlank(machineCustomPath)) {
            machineCustomPath = DOCKER_MACHINE_ARQUILLIAN_PATH;
        }
        String dockerMachineFile = HomeResolverUtil.resolveHomeDirectoryChar(
            machineCustomPath + "/" + machineVersion + "/" + DockerMachine.DOCKER_MACHINE_EXEC);
        return new File(dockerMachineFile);
    }

    public String getDockerServerUri() {
        return dockerServerUri;
    }

    public String getDockerServerVersion() {
        return dockerServerVersion;
    }

    public DockerCompositions getDockerContainersContent() {
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

    //this property is resolved in DockerCubeConfigurator class.
    public String getDockerServerIp() {
        return dockerServerIp;
    }

    public AutoStartParser getAutoStartContainers() {
        return autoStartContainers;
    }

    void setAutoStartContainers(AutoStartParser autoStartParser) {
        this.autoStartContainers = autoStartParser;
    }

    public DockerAutoStartOrder getDockerAutoStartOrder() {
        return dockerAutoStartOrder;
    }

    public boolean isClean() {
        return clean;
    }

    public boolean isRemoveVolumes() {
        return removeVolumes;
    }

    public DefinitionFormat getDefinitionFormat() {
        return definitionFormat;
    }

    public boolean isDockerInsideDockerResolution() {
        return dockerInsideDockerResolution;
    }

    public boolean isCleanBuildImage() {
        return cleanBuildImage;
    }

    @Override public String toString() {
        String lineSeparator = System.lineSeparator();
        StringBuilder content = new StringBuilder();

        content.append("CubeDockerConfiguration: ").append(lineSeparator);
        if (dockerServerVersion != null) {
            content.append("  ").append(DOCKER_VERSION).append(" = ").append(dockerServerVersion).append(lineSeparator);
        }
        if (dockerServerUri != null) {
            content.append("  ").append(DOCKER_URI).append(" = ").append(dockerServerUri).append(lineSeparator);
        }
        if (dockerRegistry != null) {
            content.append("  ").append(DOCKER_REGISTRY).append(" = ").append(dockerRegistry).append(lineSeparator);
        }
        if (boot2DockerPath != null) {
            content.append("  ").append(BOOT2DOCKER_PATH).append(" = ").append(boot2DockerPath).append(lineSeparator);
        }
        if (dockerMachinePath != null) {
            content.append("  ").append(DOCKER_MACHINE_PATH).append(" = ").append(dockerMachinePath).append(lineSeparator);
        }
        if (machineName != null) {
            content.append("  ").append(DOCKER_MACHINE_NAME).append(" = ").append(machineName).append(lineSeparator);
        }
        if (username != null) {
            content.append("  ").append(USERNAME).append(" = ").append(username).append(lineSeparator);
        }
        if (password != null) {
            content.append("  ").append(PASSWORD).append(" = ").append(password).append(lineSeparator);
        }
        if (email != null) {
            content.append("  ").append(EMAIL).append(" = ").append(email).append(lineSeparator);
        }
        if (certPath != null) {
            content.append("  ").append(CERT_PATH).append(" = ").append(certPath).append(lineSeparator);
        }

        content.append("  ").append(TLS_VERIFY).append(" = ").append(tlsVerify).append(lineSeparator);

        if (dockerServerIp != null) {
            content.append("  ").append(DOCKER_SERVER_IP).append(" = ").append(dockerServerIp).append(lineSeparator);
        }
        if (definitionFormat != null) {
            content.append("  ").append(DEFINITION_FORMAT).append(" = ").append(definitionFormat).append(lineSeparator);
        }
        if (autoStartContainers != null) {
            content.append("  ").append(AUTO_START_CONTAINERS).append(" = ").append(autoStartContainers).append(lineSeparator);
        }

        content.append("  ").append(CLEAN).append(" = ").append(clean).append(lineSeparator);

        content.append("  ").append(REMOVE_VOLUMES).append(" = ").append(removeVolumes).append(lineSeparator);

        if (dockerContainersContent != null) {
            String output = ConfigUtil.dump(dockerContainersContent);
            content.append("  ").append(DOCKER_CONTAINERS).append(" = ").append(output).append(lineSeparator);
        }

        return content.toString();
    }
}
