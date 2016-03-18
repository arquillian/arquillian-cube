package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.arquillian.cube.HostUriContext;
import org.arquillian.cube.docker.impl.util.AbstractCliInternetAddressResolver;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.GitHubUtil;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.docker.impl.util.Machine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.task.net.DownloadTool;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerConfigurator {

    private static Logger log = Logger.getLogger(CubeDockerConfigurator.class.getName());
    private static final String EXTENSION_NAME = "docker";
    private static final String UNIX_SOCKET_SCHEME = "unix";
    public static final String DOCKER_HOST = "DOCKER_HOST";
    private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";
    private static final String DOCKER_MACHINE_NAME = "DOCKER_MACHINE_NAME";

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeDockerConfiguration> configurationProducer;

    @Inject
    @ApplicationScoped
    private InstanceProducer<HostUriContext> hostUriContextInstanceProducer;

    @Inject
    private Instance<Boot2Docker> boot2DockerInstance;

    @Inject
    private Instance<DockerMachine> dockerMachineInstance;

    @Inject
    private Instance<Top> topInstance;

    @Inject
    @ApplicationScoped
    private InstanceProducer<OperatingSystemFamily> operatingSystemFamilyInstanceProducer;

    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        configure(arquillianDescriptor);
    }

    private void configure(ArquillianDescriptor arquillianDescriptor) {
        operatingSystemFamilyInstanceProducer.set(new OperatingSystemResolver().currentOperatingSystem().getFamily());
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        config = resolveSystemEnvironmentVariables(config);
        config = resolveDockerInsideDocker(config);
        config = resolveDownloadDockerMachine(config);
        config = resolveAutoStartDockerMachine(config);
        config = resolveDefaultDockerMachine(config);
        config = resolveServerUriByOperativeSystem(config);
        config = resolveServerIp(config);
        config = resolveTlsVerification(config);
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(config);
        System.out.println(cubeConfiguration);
        hostUriContextInstanceProducer.set(new HostUriContext(cubeConfiguration.getDockerServerUri()));
        configurationProducer.set(cubeConfiguration);
    }

    private Map<String,String> resolveDockerInsideDocker(Map<String, String> cubeConfiguration) {
        // if DIND_RESOLUTION property is not set, since by default is enabled, we need to go inside code.
        if (!cubeConfiguration.containsKey(CubeDockerConfiguration.DIND_RESOLUTION) || Boolean.parseBoolean(cubeConfiguration.get(CubeDockerConfiguration.DIND_RESOLUTION))) {
            if (topInstance.get().isSpinning()) {
                log.fine(String.format("Your Cube tests are going to run inside a running Docker container. %s property is replaced to %s", CubeDockerConfiguration.DOCKER_URI, OperatingSystemFamily.DIND.getServerUri()));
                String serverUri = OperatingSystemFamily.DIND.getServerUri();
                cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, serverUri);
            }
        }
        return cubeConfiguration;
    }

    private Map<String, String> resolveDownloadDockerMachine(Map<String, String> config) {
        if (config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME)) {
            final String cliPathExec = config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH);
            if (!dockerMachineInstance.get().isDockerMachineInstalled(cliPathExec)) {
                String machineVersion = GitHubUtil.getDockerMachineLatestVersion();
                String machineCustomPath = config.get(CubeDockerConfiguration.DOCKER_MACHINE_CUSTOM_PATH);
                String machineArquillianPath = CubeDockerConfiguration.resolveMachinePath(machineCustomPath, machineVersion);
                File dockerMachineFile = new File(machineArquillianPath);

                boolean dockerMachineFileExist = dockerMachineFile != null && dockerMachineFile.exists();

                String machineName = config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME);
                String machineUrl = CubeDockerConfiguration.resolveUrl(machineVersion);

                if (!dockerMachineFileExist) {
                    Spacelift.task(DownloadTool.class)
                                .from(machineUrl)
                                .to(dockerMachineFile)
                                .execute()
                                .await();
                    config.put(CubeDockerConfiguration.DOCKER_MACHINE_PATH, dockerMachineFile.getAbsolutePath());

                    dockerMachineInstance.get().grantPermissionToDockerMachine(machineArquillianPath);

                    String machineDriver = config.get(CubeDockerConfiguration.DOCKER_MACHINE_DRIVER);
                    dockerMachineInstance.get().createMachine(machineArquillianPath, machineDriver, machineName);
                } else {
                    config.put(CubeDockerConfiguration.DOCKER_MACHINE_PATH, dockerMachineFile.getAbsolutePath());
                }
            }
        }
        return config;
    }

    private Map<String,String> resolveAutoStartDockerMachine(Map<String, String> config) {

        if (config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME)) {
            final String cliPathExec = config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH);
            if (dockerMachineInstance.get().isDockerMachineInstalled(cliPathExec)) {
                String machineName = config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME);
                final Set<Machine> machines = dockerMachineInstance.get().list(cliPathExec, "name", machineName);

                if (machines.size() == 1) {
                    Machine machine = getFirstMachine(machines);
                    if (machine.getState().equalsIgnoreCase("Stopped")) {
                        dockerMachineInstance.get().startDockerMachine(cliPathExec, machineName);
                    }
                } else {
                    log.log(Level.SEVERE, String.format("You are trying to run containers in Docker Machine %s but %s Docker Machines instances are installed.", config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME), machines));
                }

            } else {
                log.log(Level.SEVERE, String.format("You are trying to run containers in Docker Machine %s but no docker-machine installed.", config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME)));
            }
        }
        return config;
    }

    private Map<String,String> resolveDefaultDockerMachine(Map<String, String> config) {

        // if user has not specified Docker URI host not a docker machine
        // setting DOCKER_URI to avoid using docker machine although it is installed
        if (!config.containsKey(CubeDockerConfiguration.DOCKER_URI) && !config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME)) {
            log.fine("No DockerUri or DockerMachine has been set, let's see if there is only one Docker Machine Running.");
            if (dockerMachineInstance.get().isDockerMachineInstalled(config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH))) {
                // we can inspect if docker machine has one and only one docker machine running, which means that would like to use that one
                Set<Machine> machines = this.dockerMachineInstance.get().list(config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH), "state", "Running");

                // if there is only one machine running we can use that one.
                // if not Cube will resolve the default URI depending on OS (linux socket, boot2docker, ...)
                if (machines.size() == 1) {
                    log.fine(String.format("One Docker Machine is running (%s) and it is going to be used for tests", machines.iterator().next().getName()));
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
        return config;
    }

    private Map<String, String> resolveServerIp(Map<String, String> config) {
        String dockerServerUri = config.get(CubeDockerConfiguration.DOCKER_URI);

        if (containsDockerHostTag(dockerServerUri)) {
            if (isDockerMachineSet(config)) {
                dockerServerUri = resolveDockerMachine(dockerServerUri, config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME), config.get(CubeDockerConfiguration.DOCKER_MACHINE_PATH));
            } else {
                dockerServerUri = resolveBoot2Docker(dockerServerUri, config.get(CubeDockerConfiguration.BOOT2DOCKER_PATH));
            }
        }

        config.put(CubeDockerConfiguration.DOCKER_URI, dockerServerUri);
        if (!config.containsKey(CubeDockerConfiguration.CERT_PATH)) {
            config.put(CubeDockerConfiguration.CERT_PATH, HomeResolverUtil.resolveHomeDirectoryChar(getDefaultTlsDirectory(config)));
        }

        resolveDockerServerIp(config, dockerServerUri);

        return config;
    }

    private Map<String, String> resolveTlsVerification(Map<String, String> config) {

        config.put(CubeDockerConfiguration.TLS_VERIFY, "true");

        if (new OperatingSystemResolver().currentOperatingSystem().getFamily() == OperatingSystemFamily.LINUX) {

            String dockerServerIp = config.get(CubeDockerConfiguration.DOCKER_SERVER_IP);

            if (isDockerMachineSet(config)) {
                if (dockerServerIp.equals("127.0.0.1") || dockerServerIp.equals("localhost")) {
                    config.put(CubeDockerConfiguration.TLS_VERIFY, "false");
                } else {
                    config.put(CubeDockerConfiguration.TLS_VERIFY, "true");
                }

                return config;
            }

            config.put(CubeDockerConfiguration.TLS_VERIFY, "false");
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
        return SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_HOST) != null;
    }
    private boolean isDockerCertPathSet() {
        return SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_CERT_PATH) != null;
    }
    private boolean isDockerMachineNameSet() {
        return SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_MACHINE_NAME) != null;
    }

    private boolean isDockerMachineSet(Map<String, String> config) {
        return config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME);
    }

    private String resolveDockerMachine(String tag, String machineName, String dockerMachinePath) {
        dockerMachineInstance.get().setMachineName(machineName);
        return tag.replaceAll(AbstractCliInternetAddressResolver.DOCKERHOST_TAG, dockerMachineInstance.get().ip(dockerMachinePath, false));
    }

    private String resolveBoot2Docker(String tag,
                                      String boot2DockerPath) {
        return tag.replaceAll(AbstractCliInternetAddressResolver.DOCKERHOST_TAG, boot2DockerInstance.get().ip(boot2DockerPath, false));
    }

    private String getDefaultTlsDirectory(Map<String, String> config) {
        if (isDockerMachineSet(config)) {
            return "~" + File.separator + ".docker" + File.separator + "machine" + File.separator + "machines" + File.separator + config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME);
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
                OperatingSystemFamily operatingSystemFamily = operatingSystemFamilyInstanceProducer.get();
                String serverUri = operatingSystemFamily.getServerUri();
                cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, serverUri);
            }
        }
        return cubeConfiguration;
    }

}
