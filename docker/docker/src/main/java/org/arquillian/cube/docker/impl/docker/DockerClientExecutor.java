package org.arquillian.cube.docker.impl.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.command.TopContainerResponse;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthConfigurations;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ChangeLog;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import org.apache.http.conn.UnsupportedSchemeException;
import org.arquillian.cube.TopContainer;
import org.arquillian.cube.docker.impl.await.StatsLogsResultCallback;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.BuildImage;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.IPAMConfig;
import org.arquillian.cube.docker.impl.client.config.Image;
import org.arquillian.cube.docker.impl.client.config.Network;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.util.BindingUtil;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.spi.CubeOutput;

import javax.ws.rs.ProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerClientExecutor {

    public static final String PATH_IN_CONTAINER = "pathInContainer";
    public static final String PATH_ON_HOST = "pathOnHost";
    public static final String C_GROUP_PERMISSIONS = "cGroupPermissions";
    public static final String PORTS_SEPARATOR = BindingUtil.PORTS_SEPARATOR;
    public static final String TAG_SEPARATOR = ":";
    public static final String RESTART_POLICY = "restartPolicy";
    public static final String CAP_DROP = "capDrop";
    public static final String CAP_ADD = "capAdd";
    public static final String DEVICES = "devices";
    public static final String DNS_SEARCH = "dnsSearch";
    public static final String NETWORK_MODE = "networkMode";
    public static final String PUBLISH_ALL_PORTS = "publishAllPorts";
    public static final String PRIVILEGED = "privileged";
    public static final String PORT_BINDINGS = "portBindings";
    public static final String LINKS = "links";
    public static final String BINDS = "binds";
    public static final String VOLUMES_FROM = "volumesFrom";
    public static final String VOLUMES = "volumes";
    public static final String DNS = "dns";
    public static final String CMD = "cmd";
    public static final String ENV = "env";
    public static final String EXPOSED_PORTS = "exposedPorts";
    public static final String ATTACH_STDERR = "attachStderr";
    public static final String ATTACH_STDIN = "attachStdin";
    public static final String CPU_SHARES = "cpuShares";
    public static final String MEMORY_SWAP = "memorySwap";
    public static final String MEMORY_LIMIT = "memoryLimit";
    public static final String STDIN_ONCE = "stdinOnce";
    public static final String STDIN_OPEN = "stdinOpen";
    public static final String TTY = "tty";
    public static final String USER = "user";
    public static final String PORT_SPECS = "portSpecs";
    public static final String HOST_NAME = "hostName";
    public static final String DISABLE_NETWORK = "disableNetwork";
    public static final String WORKING_DIR = "workingDir";
    public static final String IMAGE = "image";
    public static final String BUILD_IMAGE = "buildImage";
    public static final String DOCKERFILE_LOCATION = "dockerfileLocation";
    public static final String NO_CACHE = "noCache";
    public static final String REMOVE = "remove";
    public static final String ALWAYS_PULL = "alwaysPull";
    public static final String ENTRYPOINT = "entryPoint";
    public static final String CPU_SET = "cpuSet";
    public static final String DOCKERFILE_NAME = "dockerfileName";
    public static final String EXTRA_HOSTS = "extraHosts";
    public static final String READ_ONLY_ROOT_FS = "ReadonlyRootfs";
    public static final String LABELS = "labels";
    public static final String DOMAINNAME = "domainName";
    private static final String DEFAULT_C_GROUPS_PERMISSION = "rwm";
    private static final Logger log = Logger.getLogger(DockerClientExecutor.class.getName());
    private static final Pattern IMAGEID_PATTERN = Pattern.compile(".*Successfully built\\s(\\p{XDigit}+)");
    private final URI dockerUri;
    private final String dockerServerIp;
    private DockerClient dockerClient;
    private CubeDockerConfiguration cubeConfiguration;
    private DockerClientConfig dockerClientConfig;

    //this should be removed in the future it is only a hack to avoid some errors with Hijack is incompatible with use of CloseNotifier.
    // It seems to be a problem with go and should be fixed in go 1.6 (and maybe in Docker 1.11.0). #320
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public DockerClientExecutor(CubeDockerConfiguration cubeConfiguration) {

        final DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig
            .createDefaultConfigBuilder();

        String dockerServerUri = cubeConfiguration.getDockerServerUri();

        dockerUri = URI.create(dockerServerUri);
        dockerServerIp = cubeConfiguration.getDockerServerIp();

        configBuilder.withApiVersion(cubeConfiguration.getDockerServerVersion())
            .withDockerHost(dockerUri.toString());

        if (cubeConfiguration.getUsername() != null) {
            configBuilder.withRegistryUsername(cubeConfiguration.getUsername());
        }

        if (cubeConfiguration.getPassword() != null) {
            configBuilder.withRegistryPassword(cubeConfiguration.getPassword());
        }

        if (cubeConfiguration.getEmail() != null) {
            configBuilder.withRegistryEmail(cubeConfiguration.getEmail());
        }

        if (cubeConfiguration.getDockerRegistry() != null) {
            configBuilder.withRegistryUrl(cubeConfiguration.getDockerRegistry());
        }

        if (cubeConfiguration.getCertPath() != null) {
            configBuilder.withDockerCertPath(HomeResolverUtil.resolveHomeDirectoryChar(cubeConfiguration.getCertPath()));
        }

        configBuilder.withDockerTlsVerify(cubeConfiguration.getTlsVerify());

        this.dockerClientConfig = configBuilder.build();
        this.cubeConfiguration = cubeConfiguration;

        this.dockerClient = buildDockerClient();
    }

    public static String getImageId(String fullLog) {
        Matcher m = IMAGEID_PATTERN.matcher(fullLog);
        String imageId = null;
        if (m.find()) {
            imageId = m.group(1);
        }
        return imageId;
    }

    private static final Device[] toDevices(Collection<org.arquillian.cube.docker.impl.client.config.Device> deviceList) {
        Device[] devices = new Device[deviceList.size()];

        int i = 0;
        for (org.arquillian.cube.docker.impl.client.config.Device device : deviceList) {
            if (device.getPathOnHost() != null
                && device.getPathInContainer() != null) {

                String cGroupPermissions;
                if (device.getcGroupPermissions() != null) {
                    cGroupPermissions = device.getcGroupPermissions();
                } else {
                    cGroupPermissions = DEFAULT_C_GROUPS_PERMISSION;
                }

                String pathOnHost = device.getPathOnHost();
                String pathInContainer = device.getPathInContainer();

                devices[i] = new Device(cGroupPermissions, pathInContainer, pathOnHost);
                i++;
            }
        }

        return devices;
    }

    private static final RestartPolicy toRestartPolicy(
        org.arquillian.cube.docker.impl.client.config.RestartPolicy restart) {
        if (restart.getName() != null) {
            String name = restart.getName();

            if ("failure".equals(name)) {
                return RestartPolicy.onFailureRestart(restart.getMaximumRetryCount());
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

    private static final Link[] toLinks(Collection<org.arquillian.cube.docker.impl.client.config.Link> linkList) {
        Link[] links = new Link[linkList.size()];
        int i = 0;
        for (org.arquillian.cube.docker.impl.client.config.Link link : linkList) {
            links[i] = new Link(link.getName(), link.getAlias());
            i++;
        }

        return links;
    }

    private static final Capability[] toCapability(Collection<String> configuredCapabilities) {
        List<Capability> capabilities = new ArrayList<Capability>();
        for (String capability : configuredCapabilities) {
            capabilities.add(Capability.valueOf(capability));
        }
        return capabilities.toArray(new Capability[capabilities.size()]);
    }

    private static final Bind[] toBinds(Collection<String> bindsList) {

        Bind[] binds = new Bind[bindsList.size()];
        int i = 0;
        for (String bind : bindsList) {
            binds[i] = Bind.parse(bind);
            i++;
        }

        return binds;
    }

    private static final Volume[] toVolumes(Collection<String> volumesList) {
        Volume[] volumes = new Volume[volumesList.size()];

        int i = 0;
        for (String volume : volumesList) {
            String[] volumeSection = volume.split(":");

            if (volumeSection.length == 2 || volumeSection.length == 3) {
                volumes[i] = new Volume(volumeSection[1]);
            } else {
                volumes[i] = new Volume(volumeSection[0]);
            }
            i++;
        }

        return volumes;
    }

    private static final VolumesFrom[] toVolumesFrom(Collection<String> volumesFromList) {

        VolumesFrom[] volumesFrom = new VolumesFrom[volumesFromList.size()];

        int i = 0;
        for (String volumesFromm : volumesFromList) {
            volumesFrom[i] = VolumesFrom.parse(volumesFromm);
            i++;
        }
        return volumesFrom;
    }

    public DockerClient buildDockerClient() {
        return DockerClientBuilder.getInstance(dockerClientConfig).build();
    }

    public List<Container> listRunningContainers() {
        this.readWriteLock.readLock().lock();
        try {
            return this.dockerClient.listContainersCmd().exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public String createContainer(String name, CubeContainer containerConfiguration) {
        String image = getImageName(containerConfiguration, name);

        try {
            this.readWriteLock.readLock().lock();

            CreateContainerCmd createContainerCmd = this.dockerClient.createContainerCmd(image);
            createContainerCmd.withName(name);

            Set<ExposedPort> allExposedPorts = resolveExposedPorts(containerConfiguration, createContainerCmd);
            if (!allExposedPorts.isEmpty()) {
                int numberOfExposedPorts = allExposedPorts.size();
                createContainerCmd.withExposedPorts(allExposedPorts.toArray(new ExposedPort[numberOfExposedPorts]));
            }

            if (containerConfiguration.getReadonlyRootfs() != null) {
                createContainerCmd.withReadonlyRootfs(containerConfiguration.getReadonlyRootfs());
            }

            if (containerConfiguration.getLabels() != null) {
                createContainerCmd.withLabels(containerConfiguration.getLabels());
            }

            if (containerConfiguration.getWorkingDir() != null) {
                createContainerCmd.withWorkingDir(containerConfiguration.getWorkingDir());
            }

            if (containerConfiguration.getDisableNetwork() != null) {
                createContainerCmd.withNetworkDisabled(containerConfiguration.getDisableNetwork());
            }

            if (containerConfiguration.getHostName() != null) {
                createContainerCmd.withHostName(containerConfiguration.getHostName());
            }

            if (containerConfiguration.getPortSpecs() != null) {
                createContainerCmd.withPortSpecs(containerConfiguration.getPortSpecs().toArray(new String[0]));
            }

            if (containerConfiguration.getUser() != null) {
                createContainerCmd.withUser(containerConfiguration.getUser());
            }

            if (containerConfiguration.getTty() != null) {
                createContainerCmd.withTty(containerConfiguration.getTty());
            }
            if (containerConfiguration.getStdinOpen() != null) {
                createContainerCmd.withStdinOpen(containerConfiguration.getStdinOpen());
            }

            if (containerConfiguration.getStdinOnce() != null) {
                createContainerCmd.withStdInOnce(containerConfiguration.getStdinOnce());
            }

            if (containerConfiguration.getMemoryLimit() != null) {
                createContainerCmd.withMemory(containerConfiguration.getMemoryLimit());
            }

            if (containerConfiguration.getMemorySwap() != null) {
                createContainerCmd.withMemorySwap(containerConfiguration.getMemorySwap());
            }

            if (containerConfiguration.getShmSize() != null) {
                createContainerCmd.getHostConfig().withShmSize(containerConfiguration.getShmSize());
            }

            if (containerConfiguration.getCpuShares() != null) {
                createContainerCmd.withCpuShares(containerConfiguration.getCpuShares());
            }

            if (containerConfiguration.getCpuSet() != null) {
                createContainerCmd.withCpusetCpus(containerConfiguration.getCpuSet());
            }

            if (containerConfiguration.getCpuQuota() != null) {
                createContainerCmd.getHostConfig().withCpuQuota(containerConfiguration.getCpuQuota());
            }

            if (containerConfiguration.getAttachStdin() != null) {
                createContainerCmd.withAttachStdin(containerConfiguration.getAttachStdin());
            }

            if (containerConfiguration.getAttachSterr() != null) {
                createContainerCmd.withAttachStderr(containerConfiguration.getAttachSterr());
            }

            if (containerConfiguration.getEnv() != null) {
                createContainerCmd.withEnv(
                    resolveDockerServerIpInList(containerConfiguration.getEnv()).toArray(new String[0]));
            }

            if (containerConfiguration.getCmd() != null) {
                createContainerCmd.withCmd(containerConfiguration.getCmd().toArray(new String[0]));
            }

            if (containerConfiguration.getDns() != null) {
                createContainerCmd.withDns(containerConfiguration.getDns().toArray(new String[0]));
            }

            if (containerConfiguration.getVolumes() != null) {
                createContainerCmd.withVolumes(toVolumes(containerConfiguration.getVolumes()));
            }

            if (containerConfiguration.getVolumesFrom() != null) {
                createContainerCmd.withVolumesFrom(toVolumesFrom(containerConfiguration.getVolumesFrom()));
            }

            if (containerConfiguration.getBinds() != null) {
                createContainerCmd.withBinds(toBinds(containerConfiguration.getBinds()));
            }

            // Dependencies is precedence over links
            if (containerConfiguration.getLinks() != null && containerConfiguration.getDependsOn() == null) {
                createContainerCmd.withLinks(toLinks(containerConfiguration.getLinks()));
            }

            if (containerConfiguration.getPortBindings() != null) {
                createContainerCmd.withPortBindings(toPortBindings(containerConfiguration.getPortBindings()));
            }

            if (containerConfiguration.getPrivileged() != null) {
                createContainerCmd.withPrivileged(containerConfiguration.getPrivileged());
            }

            if (containerConfiguration.getPublishAllPorts() != null) {
                createContainerCmd.withPublishAllPorts(containerConfiguration.getPublishAllPorts());
            }

            if (containerConfiguration.getNetworkMode() != null) {
                createContainerCmd.withNetworkMode(containerConfiguration.getNetworkMode());
            }

            if (containerConfiguration.getDnsSearch() != null) {
                createContainerCmd.withDnsSearch(containerConfiguration.getDnsSearch().toArray(new String[0]));
            }

            if (containerConfiguration.getDevices() != null) {
                createContainerCmd.withDevices(toDevices(containerConfiguration.getDevices()));
            }

            if (containerConfiguration.getRestartPolicy() != null) {
                createContainerCmd.withRestartPolicy(toRestartPolicy(containerConfiguration.getRestartPolicy()));
            }

            if (containerConfiguration.getCapAdd() != null) {
                createContainerCmd.withCapAdd(toCapability(containerConfiguration.getCapAdd()));
            }

            if (containerConfiguration.getCapDrop() != null) {
                createContainerCmd.withCapDrop(toCapability(containerConfiguration.getCapDrop()));
            }

            if (containerConfiguration.getExtraHosts() != null) {
                createContainerCmd.withExtraHosts(containerConfiguration.getExtraHosts().toArray(new String[0]));
            }
            if (containerConfiguration.getEntryPoint() != null) {
                createContainerCmd.withEntrypoint(containerConfiguration.getEntryPoint().toArray(new String[0]));
            }

            if (containerConfiguration.getDomainName() != null) {
                createContainerCmd.withDomainName(containerConfiguration.getDomainName());
            }

            if (containerConfiguration.getIpv4Address() != null) {
                createContainerCmd.withIpv4Address(containerConfiguration.getIpv4Address());
            }

            if (containerConfiguration.getIpv6Address() != null) {
                createContainerCmd.withIpv6Address(containerConfiguration.getIpv6Address());
            }

            if (containerConfiguration.getAliases() != null) {
                createContainerCmd.withAliases(containerConfiguration.getAliases().toArray(new String[0]));
            }

            boolean alwaysPull = false;

            if (containerConfiguration.getAlwaysPull() != null) {
                alwaysPull = containerConfiguration.getAlwaysPull();
            }

            if (alwaysPull) {
                log.info(String.format(
                    "Pulling latest Docker Image %s.", image));
                this.pullImage(image);
            }

            try {
                return createContainerCmd.exec().getId();
            } catch (NotFoundException e) {
                if (!alwaysPull) {
                    log.warning(String.format(
                        "Docker Image %s is not on DockerHost and it is going to be automatically pulled.", image));
                    this.pullImage(image);
                    return createContainerCmd.exec().getId();
                } else {
                    throw e;
                }
            } catch (ConflictException e) {
                if (cubeConfiguration.isClean()) {
                    log.warning(String.format("Container name %s is already use. Since clean mode is enabled, " +
                        "container is going to be self removed.", name));
                    try {
                        this.stopContainer(name);
                    } catch (NotModifiedException e1) {
                        // Container was already stopped
                    }
                    this.removeContainer(name, containerConfiguration.getRemoveVolumes());
                    return createContainerCmd.exec().getId();
                } else {
                    throw e;
                }
            } catch (ProcessingException e) {
                if (e.getCause() instanceof UnsupportedSchemeException) {
                    if (e.getCause().getMessage().contains("https")) {
                        throw new IllegalStateException("You have configured serverUri with https protocol but " +
                            "certPath property is missing or points out to an invalid certificate to handle the SSL.",
                            e.getCause());
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    private List<String> resolveDockerServerIpInList(Collection<String> envs) {
        List<String> resolvedEnv = new ArrayList<String>();
        for (String env : envs) {
            if (env.contains(CubeDockerConfiguration.DOCKER_SERVER_IP)) {
                resolvedEnv.add(
                    env.replaceAll(CubeDockerConfiguration.DOCKER_SERVER_IP, cubeConfiguration.getDockerServerIp()));
            } else {
                resolvedEnv.add(env);
            }
        }
        return resolvedEnv;
    }

    private Set<ExposedPort> resolveExposedPorts(CubeContainer containerConfiguration,
        CreateContainerCmd createContainerCmd) {
        Set<ExposedPort> allExposedPorts = new HashSet<>();
        if (containerConfiguration.getPortBindings() != null) {
            for (PortBinding binding : containerConfiguration.getPortBindings()) {
                allExposedPorts.add(new ExposedPort(binding.getExposedPort().getExposed(),
                    InternetProtocol.parse(binding.getExposedPort().getType())));
            }
        }
        if (containerConfiguration.getExposedPorts() != null) {
            for (org.arquillian.cube.docker.impl.client.config.ExposedPort port : containerConfiguration.getExposedPorts()) {
                allExposedPorts.add(new ExposedPort(port.getExposed(), InternetProtocol.parse(port.getType())));
            }
        }
        return allExposedPorts;
    }

    private String getImageName(CubeContainer containerConfiguration, String name) {
        String image;

        if (containerConfiguration.getImage() != null) {
            image = containerConfiguration.getImage().toImageRef();
        } else {

            if (containerConfiguration.getBuildImage() != null) {

                BuildImage buildImage = containerConfiguration.getBuildImage();

                if (buildImage.getDockerfileLocation() != null) {
                    Map<String, Object> params = new HashMap<String, Object>(); //(containerConfiguration, BUILD_IMAGE);
                    params.put("noCache", buildImage.isNoCache());
                    params.put("remove", buildImage.isRemove());
                    params.put("dockerFileLocation", buildImage.getDockerfileLocation());
                    params.put("dockerFileName", buildImage.getDockerfileName());

                    image = this.buildImage(buildImage.getDockerfileLocation(), name, params);
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

    public void startContainer(String id, CubeContainer containerConfiguration) {
        this.readWriteLock.readLock().lock();
        try {
            StartContainerCmd startContainerCmd = this.dockerClient.startContainerCmd(id);

            startContainerCmd.exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public Statistics statsContainer(String id) throws IOException {
        this.readWriteLock.readLock().lock();

        try {
            StatsCmd statsCmd = this.dockerClient.statsCmd(id);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            StatsLogsResultCallback statslogs = new StatsLogsResultCallback(countDownLatch);
            try {
                StatsLogsResultCallback statscallback = statsCmd.exec(statslogs);
                countDownLatch.await(5, TimeUnit.SECONDS);
                statscallback.close();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
            return statslogs.getStatistics();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    private Ports toPortBindings(Collection<PortBinding> portBindings) {
        Ports ports = new Ports();
        for (PortBinding portBinding : portBindings) {
            ports.bind(
                new ExposedPort(
                    portBinding.getExposedPort().getExposed(),
                    InternetProtocol.parse(portBinding.getExposedPort().getType())),
                new Binding(portBinding.getHost(), Integer.toString(portBinding.getBound())));
        }
        return ports;
    }

    public void killContainer(String containerId) {
        this.readWriteLock.readLock().lock();
        try {
            this.dockerClient.killContainerCmd(containerId).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void stopContainer(String containerId) {
        this.readWriteLock.readLock().lock();
        try {
            this.dockerClient.stopContainerCmd(containerId).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void removeContainer(String containerId, boolean removeVolumes) {
        this.readWriteLock.readLock().lock();
        try {
            this.dockerClient.removeContainerCmd(containerId).withRemoveVolumes(removeVolumes).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public InspectContainerResponse inspectContainer(String containerId) {
        this.readWriteLock.readLock().lock();
        try {
            return this.dockerClient.inspectContainerCmd(containerId).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public int waitContainer(String containerId) {
        this.readWriteLock.readLock().lock();
        try {
            return this.dockerClient.waitContainerCmd(containerId)
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public Version dockerHostVersion() {
        this.readWriteLock.readLock().lock();
        try {
            return this.dockerClient.versionCmd().exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void pingDockerServer() {
        this.readWriteLock.readLock().lock();
        try {
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
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    private String buildImage(String location, String name, Map<String, Object> params) {

        this.readWriteLock.writeLock().lock();
        try {
            BuildImageCmd buildImageCmd = createBuildCommand(location);
            configureBuildCommand(params, buildImageCmd);
            if (name != null) {
                buildImageCmd.withTag(name);
            }
            String imageId = buildImageCmd.exec(new BuildImageResultCallback()).awaitImageId();

            if (imageId == null) {
                throw new IllegalStateException(
                    String.format(
                        "Docker server has not provided an imageId for image build from %s.",
                        location));
            }

            // TODO this should be removed in the future it is only a hack to avoid some errors with Hijack is incompatible with use of CloseNotifier.
            // It seems to be a problem with go and should be fixed in go 1.6 (and maybe in Docker 1.11.0).
            // To test in future versions we only need to comment the close + recreation.
            // following lines fixes #310 by closing and rebuilding dockerClient
            // https://github.com/arquillian/arquillian-cube/issues/322
            try {
                this.dockerClient.close();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            this.dockerClient = buildDockerClient();

            return imageId.trim();
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public void removeImage(String contaierID, Boolean force) {
        this.readWriteLock.readLock().lock();
        try {

            this.dockerClient.removeImageCmd(contaierID).withForce(force).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    private void configureBuildCommand(Map<String, Object> params, BuildImageCmd buildImageCmd) {
        if (params.containsKey(NO_CACHE)) {
            buildImageCmd.withNoCache((boolean) params.get(NO_CACHE));
        }

        if (params.containsKey(REMOVE)) {
            buildImageCmd.withRemove((boolean) params.get(REMOVE));
        }

        if (params.containsKey(DOCKERFILE_NAME)) {
            buildImageCmd.withDockerfile(new File((String) params.get(DOCKERFILE_NAME)));
        }
        
        if(this.dockerClientConfig.getRegistryUsername() != null && this.dockerClientConfig.getRegistryPassword() != null){
            AuthConfig buildAuthConfig = new AuthConfig().withUsername(this.dockerClientConfig.getRegistryUsername())
                    .withPassword(this.dockerClientConfig.getRegistryPassword())
                    .withEmail(this.dockerClientConfig.getRegistryEmail())
                    .withRegistryAddress(this.dockerClientConfig.getRegistryUrl());
            final AuthConfigurations authConfigurations = new AuthConfigurations();
            authConfigurations.addConfig(buildAuthConfig);
            buildImageCmd.withBuildAuthConfigs(authConfigurations);
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

    public String containerLog(String containerId) {
        this.readWriteLock.readLock().lock();

        try {
            return dockerClient.logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .exec(new LogContainerTestCallback()).awaitCompletion().toString();

        } catch (InterruptedException e) {
            log.log(Level.SEVERE, e.getMessage());
            return "";
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public static class LogContainerTestCallback extends LogContainerResultCallback {
        protected final StringBuilder log = new StringBuilder();

        List<Frame> collectedFrames = new ArrayList<>();

        boolean collectFrames = false;

        public LogContainerTestCallback() {
            this(false);
        }

        public LogContainerTestCallback(boolean collectFrames) {
            this.collectFrames = collectFrames;
        }

        @Override
        public void onNext(Frame frame) {
            if(collectFrames) collectedFrames.add(frame);
            log.append(new String(frame.getPayload()));
        }

        @Override
        public String toString() {
            return log.toString();
        }


        public List<Frame> getCollectedFrames() {
            return collectedFrames;
        }
    }

    public void pullImage(String imageName) {

        this.readWriteLock.readLock().lock();

        try {
            final Image image = Image.valueOf(imageName);

            PullImageCmd pullImageCmd = this.dockerClient.pullImageCmd(image.getName());

            String tag = image.getTag();
            if (tag != null && !"".equals(tag)) {
                pullImageCmd.withTag(tag);
            } else {
                pullImageCmd.withTag("latest");
            }

            pullImageCmd.exec(new PullImageResultCallback()).awaitSuccess();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public CubeOutput execStart(String containerId, String... commands) {
        this.readWriteLock.readLock().lock();
        try {
            String id = execCreate(containerId, commands);
            return execStartOutput(id);
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void execStartDetached(String containerId, String... commands) {
        this.readWriteLock.readLock().lock();
        try {
            String id = execCreate(containerId, commands);
            this.dockerClient.execStartCmd(id).withDetach(true).exec(new ExecStartResultCallback());
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    /**
     * EXecutes command to given container returning the inspection object as well. This method does 3 calls to
     * dockerhost. Create, Start and Inspect.
     *
     * @param containerId
     *     to execute command.
     */
    public ExecInspection execStartVerbose(String containerId, String... commands) {
        this.readWriteLock.readLock().lock();
        try {
            String id = execCreate(containerId, commands);
            CubeOutput output = execStartOutput(id);

            return new ExecInspection(output, inspectExec(id));
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    private InspectExecResponse inspectExec(String id) {
        final InspectExecResponse exec = this.dockerClient.inspectExecCmd(id).exec();
        return exec;
    }

    private String execCreate(String containerId, String... commands) {
        ExecCreateCmdResponse execCreateCmdResponse = this.dockerClient.execCreateCmd(containerId)
            .withAttachStdout(true).withAttachStdin(true).withAttachStderr(true).withTty(false).withCmd(commands)
            .exec();

        return execCreateCmdResponse.getId();
    }

    private CubeOutput execStartOutput(String id) {
        OutputStream outputStream = new ByteArrayOutputStream();
        OutputStream errorStream = new ByteArrayOutputStream();
        try {
            dockerClient.execStartCmd(id).withDetach(false)
                .exec(new ExecStartResultCallback(outputStream, errorStream)).awaitCompletion();
        } catch (InterruptedException e) {
            return new CubeOutput("", "");
        }

        return new CubeOutput(outputStream.toString(), errorStream.toString());
    }

    public List<org.arquillian.cube.ChangeLog> inspectChangesOnContainerFilesystem(String containerId) {
        this.readWriteLock.readLock().lock();
        try {
            List<ChangeLog> changeLogs = dockerClient.containerDiffCmd(containerId).exec();
            List<org.arquillian.cube.ChangeLog> changes = new ArrayList<>();
            for (ChangeLog changeLog : changeLogs) {
                changes.add(new org.arquillian.cube.ChangeLog(changeLog.getPath(), changeLog.getKind()));
            }
            return changes;
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public TopContainer top(String containerId) {
        this.readWriteLock.readLock().lock();
        try {
            TopContainerResponse topContainer = dockerClient.topContainerCmd(containerId).exec();
            return new TopContainer(topContainer.getTitles(), topContainer.getProcesses());
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public InputStream getFileOrDirectoryFromContainerAsTar(String containerId, String from) {
        this.readWriteLock.readLock().lock();
        try {
            InputStream response = dockerClient.copyFileFromContainerCmd(containerId, from).exec();
            return response;
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void copyStreamToContainer(String containerId, File from) {
        this.readWriteLock.readLock().lock();
        try {
            dockerClient.copyArchiveToContainerCmd(containerId).withHostResource(from.getAbsolutePath()).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void copyStreamToContainer(String containerId, File from, File to) {
        this.readWriteLock.readLock().lock();
        try {
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withRemotePath(to.getAbsolutePath())
                .withHostResource(from.getAbsolutePath()).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void connectToNetwork(String networkId, String containerID) {
        this.readWriteLock.readLock().lock();
        try {
            this.dockerClient.connectToNetworkCmd().withNetworkId(networkId).withContainerId(containerID).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void copyLog(String containerId, boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail,
        OutputStream outputStream) throws IOException {
        this.readWriteLock.readLock().lock();
        try {
            LogContainerCmd logContainerCmd =
                dockerClient.logContainerCmd(containerId).withStdErr(false).withStdOut(false);

            logContainerCmd.withFollowStream(follow);
            logContainerCmd.withStdOut(stdout);
            logContainerCmd.withStdErr(stderr);
            logContainerCmd.withTimestamps(timestamps);

            if (tail < 0) {
                logContainerCmd.withTailAll();
            } else {
                logContainerCmd.withTail(tail);
            }

            OutputStreamLogsResultCallback outputStreamLogsResultCallback =
                new OutputStreamLogsResultCallback(outputStream);
            logContainerCmd.exec(outputStreamLogsResultCallback);
            try {
                outputStreamLogsResultCallback.awaitCompletion();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        } finally {
            this.readWriteLock.readLock().unlock();
        }
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

    public String createNetwork(String id, Network network) {
        this.readWriteLock.readLock().lock();
        try {
            final CreateNetworkCmd createNetworkCmd = this.dockerClient.createNetworkCmd().withName(id);

            if (network.getDriver() != null) {
                createNetworkCmd.withDriver(network.getDriver());
            }

            if (network.getIpam() != null) {
                createNetworkCmd.withIpam(new com.github.dockerjava.api.model.Network.Ipam().withConfig(
                    createIpamConfig(network)));
            }

            if (network.getOptions() != null && !network.getOptions().isEmpty()) {
                createNetworkCmd.withOptions(network.getOptions());
            }

            final CreateNetworkResponse exec = createNetworkCmd.exec();
            return exec.getId();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public void removeNetwork(String id) {
        this.readWriteLock.readLock().lock();
        try {
            this.dockerClient.removeNetworkCmd(id).exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public List<com.github.dockerjava.api.model.Network> getNetworks() {
        this.readWriteLock.readLock().lock();
        try {
            return this.dockerClient.listNetworksCmd().exec();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    private List<com.github.dockerjava.api.model.Network.Ipam.Config> createIpamConfig(Network network) {
        List<com.github.dockerjava.api.model.Network.Ipam.Config> ipamConfigs = new ArrayList<>();
        List<IPAMConfig> IPAMConfigs = network.getIpam().getIpamConfigs();

        if (IPAMConfigs != null) {
            for (IPAMConfig IpamConfig : IPAMConfigs) {
                com.github.dockerjava.api.model.Network.Ipam.Config config =
                    new com.github.dockerjava.api.model.Network.Ipam.Config();
                if (IpamConfig.getGateway() != null) {
                    config.withGateway(IpamConfig.getGateway());
                }
                if (IpamConfig.getIpRange() != null) {
                    config.withIpRange(IpamConfig.getIpRange());
                }
                if (IpamConfig.getSubnet() != null) {
                    config.withSubnet(IpamConfig.getSubnet());
                }
                ipamConfigs.add(config);
            }
        }

        return ipamConfigs;
    }

    /**
     * Get the URI of the docker host
     */
    public URI getDockerUri() {
        return dockerUri;
    }

    public DockerClient getDockerClient() {
        return this.dockerClient;
    }

    public String getDockerServerIp() {
        return dockerServerIp;
    }

    public static class ExecInspection {
        private CubeOutput output;
        private InspectExecResponse inspectExecResponse;

        public ExecInspection(CubeOutput output, InspectExecResponse inspectExecResponse) {
            this.output = output;
            this.inspectExecResponse = inspectExecResponse;
        }

        public CubeOutput getOutput() {
            return output;
        }

        public InspectExecResponse getInspectExecResponse() {
            return inspectExecResponse;
        }
    }

    private static class OutputStreamLogsResultCallback
        extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {

        private OutputStream outputStream;

        public OutputStreamLogsResultCallback(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void onNext(Frame object) {
            try {
                this.outputStream.write(object.getPayload());
                this.outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
