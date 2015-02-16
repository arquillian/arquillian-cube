package org.arquillian.cube.impl.docker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.ProcessingException;

import org.arquillian.cube.impl.client.CubeConfiguration;
import org.arquillian.cube.impl.util.BindingUtil;
import org.arquillian.cube.impl.util.CommandLineExecutor;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.impl.util.OperatingSystemResolver;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;

public class DockerClientExecutor {

    private static final String BOOT2DOCKER_EXEC = "boot2docker";
    private static final String BOOT2DOCKER_TAG = BOOT2DOCKER_EXEC;
    private static final String PORTS_SEPARATOR = BindingUtil.PORTS_SEPARATOR;
    private static final String TAG_SEPARATOR = ":";
    private static final String RESTART_POLICY = "restartPolicy";
    private static final String CAP_DROP = "capDrop";
    private static final String CAP_ADD = "capAdd";
    private static final String DEVICES = "devices";
    private static final String DNS_SEARCH = "dnsSearch";
    private static final String NETWORK_MODE = "networkMode";
    private static final String PUBLISH_ALL_PORTS = "publishAllPorts";
    private static final String PRIVILEGED = "privileged";
    private static final String PORT_BINDINGS = "portBindings";
    private static final String LINKS = "links";
    private static final String BINDS = "binds";
    private static final String VOLUMES_FROM = "volumesFrom";
    private static final String VOLUMES = "volumes";
    private static final String DNS = "dns";
    private static final String CMD = "cmd";
    private static final String ENV = "env";
    private static final String EXPOSED_PORTS = "exposedPorts";
    private static final String ATTACH_STDERR = "attachStderr";
    private static final String ATTACH_STDIN = "attachStdin";
    private static final String CPU_SHARES = "cpuShares";
    private static final String MEMORY_SWAP = "memorySwap";
    private static final String MEMORY_LIMIT = "memoryLimit";
    private static final String STDIN_ONCE = "stdinOnce";
    private static final String STDIN_OPEN = "stdinOpen";
    private static final String TTY = "tty";
    private static final String USER = "user";
    private static final String PORT_SPECS = "portSpecs";
    private static final String HOST_NAME = "hostName";
    private static final String DISABLE_NETWORK = "disableNetwork";
    private static final String WORKING_DIR = "workingDir";
    private static final String IMAGE = "image";
    private static final String BUILD_IMAGE = "buildImage";
    private static final String DOCKERFILE_LOCATION = "dockerfileLocation";
    private static final String NO_CACHE = "noCache";
    private static final String REMOVE = "remove";
    private static final String FOLLOW = "follow";
    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";
    private static final String TIMESTAMPS = "timestamps";
    private static final String TAIL = "tail";
    private static final String TO = "to";
    private static final String FROM = "from";

    private static final Pattern IMAGEID_PATTERN = Pattern.compile(".*Successfully built\\s(\\p{XDigit}+)");
    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}");

    private static final Logger log = Logger.getLogger(DockerClientExecutor.class.getName());

    private DockerClient dockerClient;
    private CubeConfiguration cubeConfiguration;
    private CommandLineExecutor commandLineExecutor;
    private OperatingSystemResolver operatingSystemResolver;
    private final URI dockerUri;

    public DockerClientExecutor(CubeConfiguration cubeConfiguration, CommandLineExecutor commandLineExecutor, OperatingSystemResolver operatingSystemResolver) {
        DockerClientConfigBuilder configBuilder =
            DockerClientConfig.createDefaultConfigBuilder();

        // TODO, if this is already set from the client natively, do we want to
        // override here?
        this.commandLineExecutor = commandLineExecutor;
        this.operatingSystemResolver = operatingSystemResolver;
        String dockerServerUri = resolveServerUri(cubeConfiguration);

        if(dockerServerUri.contains(BOOT2DOCKER_TAG)) {
            dockerServerUri = resolveBoot2Docker(dockerServerUri, cubeConfiguration);
        }

        dockerUri = URI.create(dockerServerUri);

        configBuilder.withVersion(cubeConfiguration.getDockerServerVersion()).withUri(dockerUri.toString());
        if(cubeConfiguration.getUsername() != null) {
            configBuilder.withUsername(cubeConfiguration.getUsername());
        }

        if(cubeConfiguration.getPassword() != null) {
            configBuilder.withPassword(cubeConfiguration.getPassword());
        }

        if(cubeConfiguration.getEmail() != null) {
            configBuilder.withEmail(cubeConfiguration.getEmail());
        }

        if(cubeConfiguration.getCertPath() != null) {
            configBuilder.withDockerCertPath(cubeConfiguration.getCertPath());
        }

        this.dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
        this.cubeConfiguration = cubeConfiguration;
    }

    private String resolveServerUri(CubeConfiguration cubeConfiguration) {
        if(cubeConfiguration.getDockerServerUri() == null) {
            return this.operatingSystemResolver.currentOperatingSystem().getFamily().getServerUri();
        } else {
            return cubeConfiguration.getDockerServerUri();
        }
    }
    
    private String resolveBoot2Docker(String dockerServerUri, CubeConfiguration cubeConfiguration) {
        String output = commandLineExecutor.execCommand(createBoot2DockerCommand(cubeConfiguration));
        Matcher m = IP_PATTERN.matcher(output);
        if(m.find()) {
            String ip = m.group();
            return dockerServerUri.replace(BOOT2DOCKER_TAG, ip);
        } else {
            String errorMessage = String.format("Boot2Docker command does not return a valid ip. It returned %s.", output);
            log.log(Level.SEVERE, errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String createBoot2DockerCommand(CubeConfiguration cubeConfiguration) {
        return cubeConfiguration.getBoot2DockerPath() == null ? BOOT2DOCKER_EXEC : cubeConfiguration.getBoot2DockerPath();
    }
    
    public List<Container> listRunningContainers() {
        return this.dockerClient.listContainersCmd().exec();
    }

    public String createContainer(String name, Map<String, Object> containerConfiguration) {

        // we check if Docker server is up and correctly configured.
        this.pingDockerServer();

        String image = getImageName(containerConfiguration);

        CreateContainerCmd createContainerCmd = this.dockerClient.createContainerCmd(image);
        createContainerCmd.withName(name);

        Set<ExposedPort> allExposedPorts = resolveExposedPorts(containerConfiguration, createContainerCmd);
        if (!allExposedPorts.isEmpty()) {
            int numberOfExposedPorts = allExposedPorts.size();
            createContainerCmd.withExposedPorts(allExposedPorts.toArray(new ExposedPort[numberOfExposedPorts]));
        }

        if (containerConfiguration.containsKey(WORKING_DIR)) {
            createContainerCmd.withWorkingDir(asString(containerConfiguration, WORKING_DIR));
        }

        if (containerConfiguration.containsKey(DISABLE_NETWORK)) {
            createContainerCmd.withDisableNetwork(asBoolean(containerConfiguration, DISABLE_NETWORK));
        }

        if (containerConfiguration.containsKey(HOST_NAME)) {
            createContainerCmd.withHostName(asString(containerConfiguration, HOST_NAME));
        }

        if (containerConfiguration.containsKey(PORT_SPECS)) {
            List<String> portSpecs = asListOfString(containerConfiguration, PORT_SPECS);
            createContainerCmd.withPortSpecs(portSpecs.toArray(new String[portSpecs.size()]));
        }

        if (containerConfiguration.containsKey(USER)) {
            createContainerCmd.withUser(asString(containerConfiguration, USER));
        }

        if (containerConfiguration.containsKey(TTY)) {
            createContainerCmd.withTty(asBoolean(containerConfiguration, TTY));
        }

        if (containerConfiguration.containsKey(STDIN_OPEN)) {
            createContainerCmd.withStdinOpen(asBoolean(containerConfiguration, STDIN_OPEN));
        }

        if (containerConfiguration.containsKey(STDIN_ONCE)) {
            createContainerCmd.withStdInOnce(asBoolean(containerConfiguration, STDIN_ONCE));
        }

        if (containerConfiguration.containsKey(MEMORY_LIMIT)) {
            createContainerCmd.withMemoryLimit(asInt(containerConfiguration, MEMORY_LIMIT));
        }

        if (containerConfiguration.containsKey(MEMORY_SWAP)) {
            createContainerCmd.withMemorySwap(asInt(containerConfiguration, MEMORY_SWAP));
        }

        if (containerConfiguration.containsKey(CPU_SHARES)) {
            createContainerCmd.withCpuShares(asInt(containerConfiguration, CPU_SHARES));
        }

        if (containerConfiguration.containsKey(ATTACH_STDIN)) {
            createContainerCmd.withAttachStdin(asBoolean(containerConfiguration, ATTACH_STDIN));
        }

        if (containerConfiguration.containsKey(ATTACH_STDERR)) {
            createContainerCmd.withAttachStderr(asBoolean(containerConfiguration, ATTACH_STDERR));
        }

        if (containerConfiguration.containsKey(ENV)) {
            List<String> env = asListOfString(containerConfiguration, ENV);
            createContainerCmd.withEnv(env.toArray(new String[env.size()]));
        }

        if (containerConfiguration.containsKey(CMD)) {
            List<String> cmd = asListOfString(containerConfiguration, CMD);
            createContainerCmd.withCmd(cmd.toArray(new String[cmd.size()]));
        }

        if (containerConfiguration.containsKey(DNS)) {
            List<String> dns = asListOfString(containerConfiguration, DNS);
            createContainerCmd.withDns(dns.toArray(new String[dns.size()]));
        }

        if (containerConfiguration.containsKey(VOLUMES)) {
            List<String> volumes = asListOfString(containerConfiguration, VOLUMES);
            createContainerCmd.withVolumes(toVolumes(volumes));
        }

        if (containerConfiguration.containsKey(VOLUMES_FROM)) {
            List<String> volumesFrom = asListOfString(containerConfiguration, VOLUMES_FROM);
            createContainerCmd.withVolumesFrom(volumesFrom.toArray(new String[volumesFrom.size()]));
        }

        try {
            return createContainerCmd.exec().getId();
        } catch (NotFoundException e) {
            log.warning(String.format(
                    "Docker Image %s is not on DockerHost and it is going to be automatically pulled.", image));
            this.pullImage(image);
            return createContainerCmd.exec().getId();
        }
    }

    private Set<ExposedPort> resolveExposedPorts(Map<String, Object> containerConfiguration,
            CreateContainerCmd createContainerCmd) {
        Set<ExposedPort> allExposedPorts = new HashSet<>();
        if (containerConfiguration.containsKey(PORT_BINDINGS)) {
            List<String> portBindings = asListOfString(containerConfiguration, PORT_BINDINGS);

            Ports assignPorts = assignPorts(portBindings);
            Map<ExposedPort, Binding[]> bindings = assignPorts.getBindings();
            Set<ExposedPort> exposedPorts = bindings.keySet();
            allExposedPorts.addAll(exposedPorts);
        }
        if (containerConfiguration.containsKey(EXPOSED_PORTS)) {
            Set<ExposedPort> exposedPorts = toExposedPorts(asListOfString(containerConfiguration, EXPOSED_PORTS));
            allExposedPorts.addAll(exposedPorts);
        }
        return allExposedPorts;
    }

    private String getImageName(Map<String, Object> containerConfiguration) {
        String image = null;

        if (containerConfiguration.containsKey(IMAGE)) {
            image = asString(containerConfiguration, IMAGE);
        } else {

            if (containerConfiguration.containsKey(BUILD_IMAGE)) {

                Map<String, Object> params = asMap(containerConfiguration, BUILD_IMAGE);

                if (params.containsKey(DOCKERFILE_LOCATION)) {
                    String dockerfileLocation = asString(params, DOCKERFILE_LOCATION);
                    image = this.buildImage(dockerfileLocation, params);
                } else {
                    throw new IllegalArgumentException(
                            "A tar file with Dockerfile on root or a directory with a Dockerfile should be provided.");
                }

            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "Current configuration file does not contain %s nor %s parameter and one of both should be provided.",
                                IMAGE, BUILD_IMAGE));
            }
        }
        return image;
    }

    public void startContainer(String id, Map<String, Object> containerConfiguration) {
        StartContainerCmd startContainerCmd = this.dockerClient.startContainerCmd(id);

        if (containerConfiguration.containsKey(BINDS)) {
            List<String> binds = asListOfString(containerConfiguration, BINDS);
            startContainerCmd.withBinds(toBinds(binds));
        }

        if (containerConfiguration.containsKey(LINKS)) {
            startContainerCmd.withLinks(toLinks(asListOfString(containerConfiguration, LINKS)));
        }

        if (containerConfiguration.containsKey(PORT_BINDINGS)) {
            List<String> portBindings = asListOfString(containerConfiguration, PORT_BINDINGS);

            Ports ports = assignPorts(portBindings);
            startContainerCmd.withPortBindings(ports);
        }

        if (containerConfiguration.containsKey(PRIVILEGED)) {
            startContainerCmd.withPrivileged(asBoolean(containerConfiguration, PRIVILEGED));
        }

        if (containerConfiguration.containsKey(PUBLISH_ALL_PORTS)) {
            startContainerCmd.withPublishAllPorts(asBoolean(containerConfiguration, PUBLISH_ALL_PORTS));
        }

        if (containerConfiguration.containsKey(NETWORK_MODE)) {
            startContainerCmd.withNetworkMode(asString(containerConfiguration, NETWORK_MODE));
        }

        if (containerConfiguration.containsKey(DNS_SEARCH)) {
            List<String> dnsSearch = asListOfString(containerConfiguration, DNS_SEARCH);
            startContainerCmd.withDnsSearch(dnsSearch.toArray(new String[dnsSearch.size()]));
        }

        if (containerConfiguration.containsKey(DEVICES)) {

            List<Map<String, Object>> devices = asListOfMap(containerConfiguration, DEVICES);
            startContainerCmd.withDevices(toDevices(devices));
        }

        if (containerConfiguration.containsKey(RESTART_POLICY)) {
            Map<String, Object> restart = asMap(containerConfiguration, RESTART_POLICY);
            startContainerCmd.withRestartPolicy(toRestatPolicy(restart));
        }

        if (containerConfiguration.containsKey(CAP_ADD)) {
            List<String> capAdds = asListOfString(containerConfiguration, CAP_ADD);
            startContainerCmd.withCapAdd(toCapability(capAdds));
        }

        if (containerConfiguration.containsKey(CAP_DROP)) {
            List<String> capDrop = asListOfString(containerConfiguration, CAP_DROP);
            startContainerCmd.withCapDrop(toCapability(capDrop));
        }

        startContainerCmd.exec();
    }

    private Ports assignPorts(List<String> portBindings) {
        Ports ports = new Ports();
        for (String portBinding : portBindings) {
            String[] elements = portBinding.split(PORTS_SEPARATOR);

            if (elements.length == 1) {

                log.info("Only exposed port is set and it will be used as port binding as well. " + elements[0]);

                // exposed port is only set and same port will be used as port binding.
                String exposedPortValue = elements[0].substring(0, elements[0].indexOf("/"));
                ports.bind(ExposedPort.parse(elements[0]), toBinding(exposedPortValue));
            } else {
                if (elements.length == 2) {
                    // port and exposed port are set
                    ports.bind(ExposedPort.parse(elements[1]), toBinding(elements[0]));
                }
            }

        }
        return ports;
    }

    public void stopContainer(String containerId) {
        this.dockerClient.stopContainerCmd(containerId).exec();
    }

    public void removeContainer(String containerId) {
        this.dockerClient.removeContainerCmd(containerId).exec();
    }

    public InspectContainerResponse inspectContainer(String containerId) {
        return this.dockerClient.inspectContainerCmd(containerId).exec();
    }

    public int waitContainer(String containerId) {
        return this.dockerClient.waitContainerCmd(containerId).exec();
    }

    public void pingDockerServer() {
        try {
            PingCmd pingCmd = this.dockerClient.pingCmd();
            pingCmd.exec();
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                throw new IllegalStateException(
                        String.format(
                                "Docker server is not running in %s host or it does not accept connections in tcp protocol, read https://github.com/arquillian/arquillian-cube#preliminaries to learn how to enable it.",
                                this.cubeConfiguration.getDockerServerUri()), e);
            }
        }
    }

    public String buildImage(String location, Map<String, Object> params) {

        BuildImageCmd buildImageCmd = createBuildCommand(location);
        configureBuildCommand(params, buildImageCmd);

        InputStream response = buildImageCmd.exec();

        // We must wait until InputStream is closed to know that image is build. Moreover we need the log to retrieve
        // the image id to invoke it automatically.
        // Currently this is a bit clunky but REST API does not provide any other way.
        String fullLog = IOUtil.asString(response);
        String imageId = getImageId(fullLog);

        if (imageId == null) {
            throw new IllegalStateException(
                    String.format(
                            "Docker server has not provided an imageId for image build from %s. Response from the server was:\n%s",
                            location, fullLog));
        }

        return imageId.trim();
    }

    public static String getImageId(String fullLog) {
        Matcher m = IMAGEID_PATTERN.matcher(fullLog);
        String imageId = null;
        if (m.find()) {
            imageId = m.group(1);
        }
        return imageId;
    }

    private void configureBuildCommand(Map<String, Object> params, BuildImageCmd buildImageCmd) {
        if (params.containsKey(NO_CACHE)) {
            buildImageCmd.withNoCache((boolean) params.get(NO_CACHE));
        }

        if (params.containsKey(REMOVE)) {
            buildImageCmd.withRemove((boolean) params.get(REMOVE));
        }
    }

    private BuildImageCmd createBuildCommand(String location) {
        BuildImageCmd buildImageCmd = null;

        try {
            URL url = new URL(location);
            buildImageCmd = this.dockerClient.buildImageCmd(url.openStream());
        } catch (MalformedURLException e) {
            // Means that it is not a URL so it can be a File or Directory
            File file = new File(location);

            if (file.exists()) {
                if (file.isDirectory()) {
                    buildImageCmd = this.dockerClient.buildImageCmd(file);
                } else {
                    try {
                        buildImageCmd = this.dockerClient.buildImageCmd(new FileInputStream(file));
                    } catch (FileNotFoundException notFoundFile) {
                        throw new IllegalArgumentException(notFoundFile);
                    }
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return buildImageCmd;
    }

    public void pullImage(String imageName) {

        PullImageCmd pullImageCmd = this.dockerClient.pullImageCmd(imageName);

        if (this.cubeConfiguration.getDockerRegistry() != null) {
            pullImageCmd.withRegistry(this.cubeConfiguration.getDockerRegistry());
        }

        int tagSeparator = imageName.indexOf(TAG_SEPARATOR);
        if (tagSeparator > 0) {
            pullImageCmd.withRepository(imageName.substring(0, tagSeparator));
            pullImageCmd.withTag(imageName.substring(tagSeparator + 1));
        }

        InputStream exec = pullImageCmd.exec();

        // To wait until image is pull we need to listen input stream until it is closed by the server
        // At this point we can be sure that image is already pulled.
        IOUtil.asString(exec);
    }

    public String execStart(String containerId, String... commands) {
        ExecCreateCmdResponse execCreateCmdResponse = this.dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true).withAttachStdin(false).withAttachStderr(false).withTty().withCmd(commands)
                .exec();
        InputStream consoleOutputStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).withDetach(false)
                .exec();
        String output;
        try {
            output = readDockerRawStreamToString(consoleOutputStream);
        } catch (IOException e) {
            return "";
        }
        return output;
    }

    public void copyFromContainer(String containerId, Map<String, Object> configurationParameters) throws IOException {
        String to = null;
        String from = null;
        if(configurationParameters.containsKey(TO) && configurationParameters.containsKey(FROM)) {
            to = (String) configurationParameters.get(TO);
            from = (String) configurationParameters.get(FROM);
        } else {
            throw new IllegalArgumentException(String.format("to and from property is mandatory when copying files from container %s.", containerId));
        }

        InputStream response = dockerClient.copyFileFromContainerCmd(containerId, from).exec();
        Path toPath = Paths.get(to);
        Files.createDirectories(toPath);

        IOUtil.untar(response, toPath.toFile());
    }

    public void copyLog(String containerId, Map<String, Object> configurationParameters) throws IOException {
        String to = null;
        if(configurationParameters.containsKey(TO)) {
            to = (String) configurationParameters.get(TO);
        } else {
            throw new IllegalArgumentException(String.format("to property is mandatory when getting logs from container %s.", containerId));
        }

        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId).withStdErr().withStdOut();

        if(configurationParameters.containsKey(FOLLOW)) {
            logContainerCmd.withFollowStream((boolean) configurationParameters.get(FOLLOW));
        }

        if(configurationParameters.containsKey(STDOUT)) {
            logContainerCmd.withStdOut((boolean) configurationParameters.get(STDOUT));
        }

        if(configurationParameters.containsKey(STDERR)) {
            logContainerCmd.withStdErr((boolean) configurationParameters.get(STDERR));
        }

        if(configurationParameters.containsKey(TIMESTAMPS)) {
            logContainerCmd.withTimestamps((boolean) configurationParameters.get(TIMESTAMPS));
        }

        if(configurationParameters.containsKey(TAIL)) {
            logContainerCmd.withTail((int) configurationParameters.get(TAIL));
        }

        InputStream log = logContainerCmd.exec();

        Path toPath = Paths.get(to);
        Path toDirectory = toPath.getParent();
        Files.createDirectories(toDirectory);
        readDockerRawStream(log, new FileOutputStream(toPath.toFile()));

    }

    private void readDockerRawStream(InputStream rawSteram, OutputStream outputStream) throws IOException {
        byte[] header = new byte[8];
        while (rawSteram.read(header) > 0) {
            ByteBuffer headerBuffer = ByteBuffer.wrap(header);

            // Stream type
            byte type = headerBuffer.get();
            // SKip 3 bytes
            headerBuffer.get();
            headerBuffer.get();
            headerBuffer.get();
            // Payload frame size
            int size = headerBuffer.getInt();

            byte[] streamOutputBuffer = new byte[size];
            rawSteram.read(streamOutputBuffer);
            outputStream.write(streamOutputBuffer);
        }
    }

    private String readDockerRawStreamToString(InputStream rawStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        readDockerRawStream(rawStream, output);
        return new String(output.toByteArray());
    }

    /**
     * Get the URI of the docker host
     * 
     * @return
     */
    public URI getDockerUri() {
        return dockerUri;
    }

    private static final Device[] toDevices(List<Map<String, Object>> devicesMap) {
        Device[] devices = new Device[devicesMap.size()];

        for (int i = 0; i < devices.length; i++) {
            Map<String, Object> device = devicesMap.get(i);
            if (device.containsKey("cGroupPermissions") && device.containsKey("pathOnHost")
                    && device.containsKey("pathInContainer")) {
                String cGroupPermissions = asString(device, "cGroupPermissions");
                String pathOnHost = asString(device, "pathOnHost");
                String pathInContainer = asString(device, "pathInContainer");

                devices[i] = new Device(cGroupPermissions, pathInContainer, pathOnHost);

            }
        }

        return devices;
    }

    private static final RestartPolicy toRestatPolicy(Map<String, Object> restart) {
        if (restart.containsKey("name")) {
            String name = asString(restart, "name");

            if ("failure".equals(name)) {
                return RestartPolicy.onFailureRestart(asInt(restart, "maximumRetryCount"));
            } else {
                if ("restart".equals(name)) {
                    return RestartPolicy.alwaysRestart();
                } else {
                    return RestartPolicy.noRestart();
                }
            }

        } else {
            return RestartPolicy.noRestart();
        }
    }

    private static final Binding toBinding(String port) {
        if (port.contains(TAG_SEPARATOR)) {
            String host = port.substring(0, port.lastIndexOf(TAG_SEPARATOR));
            int extractedPort = Integer.parseInt(port.substring(port.lastIndexOf(TAG_SEPARATOR) + 1, port.length()));
            return Ports.Binding(host, extractedPort);
        } else {
            return Ports.Binding(Integer.parseInt(port));
        }
    }

    private static final boolean asBoolean(Map<String, Object> map, String property) {
        return (boolean) map.get(property);
    }

    private static final int asInt(Map<String, Object> map, String property) {
        return (int) map.get(property);
    }

    private static final Link[] toLinks(List<String> linkList) {
        Link[] links = new Link[linkList.size()];
        for (int i = 0; i < links.length; i++) {
            links[i] = Link.parse(linkList.get(i));
        }

        return links;
    }

    private static final Capability[] toCapability(List<String> configuredCapabilities) {
        List<Capability> capabilities = new ArrayList<Capability>();
        for (String capability : configuredCapabilities) {
            capabilities.add(Capability.valueOf(capability));
        }
        return capabilities.toArray(new Capability[capabilities.size()]);
    }

    private static final Bind[] toBinds(List<String> bindsList) {

        Bind[] binds = new Bind[bindsList.size()];
        for (int i = 0; i < binds.length; i++) {
            binds[i] = Bind.parse(bindsList.get(i));
        }

        return binds;
    }

    private static final Set<ExposedPort> toExposedPorts(List<String> exposedPortsList) {
        Set<ExposedPort> exposedPorts = new HashSet<>();

        for (String exposedPort : exposedPortsList) {
            exposedPorts.add(ExposedPort.parse(exposedPort));
        }

        return exposedPorts;
    }

    private static final Volume[] toVolumes(List<String> volumesList) {
        Volume[] volumes = new Volume[volumesList.size()];

        for (int i = 0; i < volumesList.size(); i++) {
            volumes[i] = new Volume(volumesList.get(i));
        }

        return volumes;
    }

    @SuppressWarnings("unchecked")
    private static final List<Map<String, Object>> asListOfMap(Map<String, Object> map, String property) {
        return (List<Map<String, Object>>) map.get(property);
    }

    @SuppressWarnings("unchecked")
    private static final List<String> asListOfString(Map<String, Object> map, String property) {
        return (List<String>) map.get(property);
    }

    private static final String asString(Map<String, Object> map, String property) {
        return (String) map.get(property);
    }

    @SuppressWarnings("unchecked")
    private static final Map<String, Object> asMap(Map<String, Object> map, String property) {
        return (Map<String, Object>) map.get(property);
    }

    public DockerClient getDockerClient() {
        return this.dockerClient;
    }

}
