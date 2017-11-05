package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.AbstractCliInternetAddressResolver;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.GitHubUtil;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.docker.impl.util.Machine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.task.net.DownloadTool;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import static org.arquillian.cube.docker.impl.client.CubeDockerConfigurator.DOCKER_HOST;

public class CubeDockerConfigurationResolver {

    private static final String UNIX_SOCKET_SCHEME = "unix";
    private static final String TCP_SCHEME = "tcp";
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";
    private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";
    private static final String DOCKER_MACHINE_NAME = "DOCKER_MACHINE_NAME";
    private static Random random = new Random();
    private static Logger log = Logger.getLogger(CubeDockerConfigurationResolver.class.getName());
    private final Top top;
    private final DockerMachine dockerMachine;
    private final Boot2Docker boot2Docker;
    private final OperatingSystemFamily operatingSystemFamily;

    public CubeDockerConfigurationResolver(Top top, DockerMachine dockerMachine, Boot2Docker boot2Docker,
                                           OperatingSystemFamily operatingSystemFamily) {
        this.top = top;
        this.dockerMachine = dockerMachine;
        this.boot2Docker = boot2Docker;
        this.operatingSystemFamily = operatingSystemFamily;
    }

    /**
     * Resolves the configuration.
     *
     * @param config The specified configuration.
     * @return The resolved configuration.
     */
    public Map<String, String> resolve(Map<String, String> config) {
        config = resolveSystemEnvironmentVariables(config);
        config = resolveDockerInsideDocker(config);
        config = resolveDownloadDockerMachine(config);
        config = resolveAutoStartDockerMachine(config);
        config = resolveDefaultDockerMachine(config);
        config = resolveServerUriByOperativeSystem(config);
        config = resolveServerIp(config);
        config = resolveTlsVerification(config);
        return config;
    }

    private Map<String, String> resolveDockerInsideDocker(Map<String, String> cubeConfiguration) {
        // if DIND_RESOLUTION property is not set, since by default is enabled, we need to go inside code.
        if (!cubeConfiguration.containsKey(CubeDockerConfiguration.DIND_RESOLUTION) || Boolean.parseBoolean(
            cubeConfiguration.get(CubeDockerConfiguration.DIND_RESOLUTION))) {
            if (top.isSpinning()) {
                log.fine(String.format(
                    "Your Cube tests are going to run inside a running Docker container. %s property is replaced to %s",
                    CubeDockerConfiguration.DOCKER_URI, OperatingSystemFamily.DIND.getServerUri()));
                String serverUri = OperatingSystemFamily.DIND.getServerUri();
                cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, serverUri);
            }
        }
        return cubeConfiguration;
    }

    private Map<String, String> resolveDownloadDockerMachine(Map<String, String> config) {
        if (config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME)) {
            final String cliPathExec = config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH);
            if (!dockerMachine.isDockerMachineInstalled(cliPathExec)) {
                String machineVersion = GitHubUtil.getDockerMachineLatestVersion();
                String machineCustomPath = config.get(CubeDockerConfiguration.DOCKER_MACHINE_CUSTOM_PATH);
                File dockerMachineFile = CubeDockerConfiguration.resolveMachinePath(machineCustomPath, machineVersion);
                String dockerMachinePath = dockerMachineFile.getPath();

                boolean dockerMachineFileExist = dockerMachineFile != null && dockerMachineFile.exists();

                String machineName = config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME);
                String machineUrl = CubeDockerConfiguration.resolveUrl(machineVersion);

                if (!dockerMachineFileExist) {
                    dockerMachineFile.getParentFile().mkdirs();
                    Spacelift.task(DownloadTool.class)
                        .from(machineUrl)
                        .to(dockerMachineFile)
                        .execute()
                        .await();
                    config.put(CubeDockerConfiguration.DOCKER_MACHINE_PATH, dockerMachinePath);

                    dockerMachine.grantPermissionToDockerMachine(dockerMachinePath);

                    String machineDriver = config.get(CubeDockerConfiguration.DOCKER_MACHINE_DRIVER);
                    dockerMachine.createMachine(dockerMachinePath, machineDriver, machineName);
                } else {
                    config.put(CubeDockerConfiguration.DOCKER_MACHINE_PATH, dockerMachinePath);
                }
            }
        }
        return config;
    }

    private Map<String, String> resolveAutoStartDockerMachine(Map<String, String> config) {

        final String cliPathExec = config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH);
        if (dockerMachine.isDockerMachineInstalled(cliPathExec)) {
            final Set<Machine> allMachines = dockerMachine.list(cliPathExec);

            Optional<Machine> machine = Optional.empty();

            if (config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME)) {
                String configuredMachineName = config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME);

                machine = allMachines.stream()
                    .filter(m -> configuredMachineName.equals(m.getName()))
                    .filter(m -> "Stopped".equalsIgnoreCase(m.getState()))
                    .findFirst();

            } else {
                if (allMachines.size() == 1 && "Stopped".equalsIgnoreCase(allMachines.iterator().next().getState())) {
                    machine = Optional.of(allMachines.iterator().next());
                }
            }

            machine.ifPresent(m -> dockerMachine.startDockerMachine(cliPathExec, m.getName()));

        }

        return config;
    }

    private Map<String, String> resolveDefaultDockerMachine(Map<String, String> config) {

        // if user has not specified Docker URI host not a docker machine
        // setting DOCKER_URI to avoid using docker machine although it is installed
        if (!config.containsKey(CubeDockerConfiguration.DOCKER_URI) && !config.containsKey(
            CubeDockerConfiguration.DOCKER_MACHINE_NAME)) {
            log.fine(
                "No DockerUri or DockerMachine has been set, let's see if there is only one Docker Machine Running.");
            if (dockerMachine.isDockerMachineInstalled(config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH))) {
                // we can inspect if docker machine has one and only one docker machine running, which means that would like to use that one
                Set<Machine> machines =
                    this.dockerMachine.list(config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH), "state", "Running");

                // if there is only one machine running we can use that one.
                // if not Cube will resolve the default URI depending on OS (linux socket, boot2docker, ...)
                if (machines.size() == 1) {
                    log.fine(String.format("One Docker Machine is running (%s) and it is going to be used for tests",
                        machines.iterator().next().getName()));
                    config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, getFirstMachine(machines).getName());
                }
            }
        }

        return config;
    }

    private Machine getFirstMachine(Set<Machine> machines) {
        return machines.iterator().next();
    }

    private Map<String, String> resolveSystemEnvironmentVariables(Map<String, String> config) {
        if (!config.containsKey(CubeDockerConfiguration.DOCKER_URI) && isDockerHostSet()) {
            String dockerHostUri = SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_HOST);
            config.put(CubeDockerConfiguration.DOCKER_URI, dockerHostUri);
        }
        if (!config.containsKey(CubeDockerConfiguration.CERT_PATH) && isDockerCertPathSet()) {
            String dockerCertPath = SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_CERT_PATH);
            config.put(CubeDockerConfiguration.CERT_PATH, dockerCertPath);
        }
        if (!config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME) && isDockerMachineNameSet()) {
            String dockerMachineName = SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_MACHINE_NAME);
            config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, dockerMachineName);
        }

        if (!config.containsKey(CubeDockerConfiguration.TLS_VERIFY) && isDockerTlsVerifySet()) {
            config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.TRUE.toString());
        }

        return config;
    }

    private Map<String, String> resolveServerIp(Map<String, String> config) {
        String dockerServerUri = config.get(CubeDockerConfiguration.DOCKER_URI);

        if (containsDockerHostTag(dockerServerUri)) {
            if (isDockerMachineSet(config)) {
                dockerServerUri =
                    resolveDockerMachine(dockerServerUri, config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME),
                        config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH));
            } else {
                dockerServerUri =
                    resolveBoot2Docker(dockerServerUri, config.get(CubeDockerConfiguration.BOOT2DOCKER_PATH));
            }

            if (!config.containsKey(CubeDockerConfiguration.TLS_VERIFY)) {
                config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(true));
            }

        }

        config.put(CubeDockerConfiguration.DOCKER_URI, dockerServerUri);
        if (!config.containsKey(CubeDockerConfiguration.CERT_PATH)) {
            config.put(CubeDockerConfiguration.CERT_PATH,
                HomeResolverUtil.resolveHomeDirectoryChar(getDefaultTlsDirectory(config)));
        }

        resolveDockerServerIp(config, dockerServerUri);

        return config;
    }

    private Map<String, String> resolveTlsVerification(Map<String, String> config) {

        URI serverUri = URI.create(config.get(CubeDockerConfiguration.DOCKER_URI));
        String scheme = serverUri.getScheme();

        if (scheme.equals(HTTP_SCHEME)) {
            config.remove(CubeDockerConfiguration.TLS_VERIFY);
        }

        if (scheme.equals(HTTPS_SCHEME)) {
            config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(true));
        }

        if (scheme.equals(HTTP_SCHEME) || scheme.equals(HTTPS_SCHEME) || scheme.equals(TCP_SCHEME)) {

            if (!config.containsKey(CubeDockerConfiguration.TLS_VERIFY)) {
                config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(scheme.equals(HTTPS_SCHEME)));
            }

            try {
                // docker-java supports only tcp and unix schemes
                serverUri = new URI(TCP_SCHEME, serverUri.getSchemeSpecificPart(), serverUri.getFragment());
                config.put(CubeDockerConfiguration.DOCKER_URI, serverUri.toString());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (!config.containsKey(CubeDockerConfiguration.TLS_VERIFY)) {
            config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(true));

            if (operatingSystemFamily == OperatingSystemFamily.LINUX) {

                String dockerServerIp = config.get(CubeDockerConfiguration.DOCKER_SERVER_IP);

                if (isDockerMachineSet(config)) {

                    if (InetAddress.getLoopbackAddress().getHostAddress().equals(dockerServerIp)
                        || InetAddress.getLoopbackAddress().getHostName().equals(dockerServerIp)) {
                        config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(false));
                    } else {
                        config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(true));
                    }
                } else {
                    config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(false));
                }
            }
        }

        if (Boolean.FALSE.toString().equals(config.get(CubeDockerConfiguration.TLS_VERIFY))) {
            config.remove(CubeDockerConfiguration.CERT_PATH);
        }

        return config;
    }

    private boolean containsDockerHostTag(String dockerServerUri) {
        return dockerServerUri.contains(AbstractCliInternetAddressResolver.DOCKERHOST_TAG);
    }

    private void resolveDockerServerIp(Map<String, String> config, String dockerServerUri) {
        URI serverUri = URI.create(dockerServerUri);
        String serverIp = UNIX_SOCKET_SCHEME.equalsIgnoreCase(serverUri.getScheme()) ? "localhost" : serverUri.getHost();
        config.put(CubeDockerConfiguration.DOCKER_SERVER_IP, serverIp);
    }

    private boolean isDockerHostSet() {
        return Strings.isNotNullOrEmpty(SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_HOST));
    }

    private boolean isDockerCertPathSet() {
        return Strings.isNotNullOrEmpty(SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_CERT_PATH));
    }

    private boolean isDockerTlsVerifySet() {
        return Strings.isNotNullOrEmpty(SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_TLS_VERIFY));
    }

    private boolean isDockerMachineNameSet() {
        return Strings.isNotNullOrEmpty(SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_MACHINE_NAME));
    }

    private boolean isDockerMachineSet(Map<String, String> config) {
        return config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME);
    }

    private String resolveDockerMachine(String tag, String machineName, String dockerMachinePath) {
        dockerMachine.setMachineName(machineName);
        return tag.replaceAll(AbstractCliInternetAddressResolver.DOCKERHOST_TAG,
            dockerMachine.ip(dockerMachinePath, false));
    }

    private String resolveBoot2Docker(String tag,
                                      String boot2DockerPath) {
        return tag.replaceAll(AbstractCliInternetAddressResolver.DOCKERHOST_TAG, boot2Docker.ip(boot2DockerPath, false));
    }

    private String getDefaultTlsDirectory(Map<String, String> config) {
        if (isDockerMachineSet(config)) {
            return "~"
                + File.separator
                + ".docker"
                + File.separator
                + "machine"
                + File.separator
                + "machines"
                + File.separator
                + config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME);
        } else {
            return "~" + File.separator + ".boot2docker" + File.separator + "certs" + File.separator + "boot2docker-vm";
        }
    }

    private boolean containsCertPath(Map<String, String> cubeConfiguration) {
        return cubeConfiguration.containsKey(CubeDockerConfiguration.CERT_PATH);
    }

    private Map<String, String> resolveServerUriByOperativeSystem(Map<String, String> cubeConfiguration) {
        if (!cubeConfiguration.containsKey(CubeDockerConfiguration.DOCKER_URI)) {
            if (isDockerMachineSet(cubeConfiguration)) {
                String serverUri = OperatingSystemFamily.MACHINE.getServerUri();
                cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, serverUri);
            } else {
                String serverUri = operatingSystemFamily.getServerUri();
                cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, serverUri);
            }
        }
        return cubeConfiguration;
    }
}
