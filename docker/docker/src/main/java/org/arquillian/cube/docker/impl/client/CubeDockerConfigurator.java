package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.arquillian.cube.HostUriContext;
import org.arquillian.cube.docker.impl.util.AbstractCliInternetAddressResolver;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.docker.impl.util.Machine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.spi.CubeConfiguration;
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
    @ApplicationScoped
    private InstanceProducer<OperatingSystemFamily> operatingSystemFamilyInstanceProducer;

    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        configure(arquillianDescriptor);
    }

    private void configure(ArquillianDescriptor arquillianDescriptor) {
        operatingSystemFamilyInstanceProducer.set(new OperatingSystemResolver().currentOperatingSystem().getFamily());
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        config = resolveSystemEnvironmentVariables(config);
        config = resolveDefaultDockerMachine(config);
        config = resolveServerUriByOperativeSystem(config);
        config = resolveServerUriTcpProtocol(config);
        config = resolveServerIp(config);
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(config);
        System.out.println(cubeConfiguration);
        hostUriContextInstanceProducer.set(new HostUriContext(cubeConfiguration.getDockerServerUri()));
        configurationProducer.set(cubeConfiguration);
    }

    private Map<String,String> resolveDefaultDockerMachine(Map<String, String> config) {

        // if user has not specified Docker URI host not a docker machine
        if (!config.containsKey(CubeDockerConfiguration.DOCKER_URI) && !config.containsKey(CubeDockerConfiguration.DOCKER_MACHINE_NAME)) {
            log.fine("No DockerUri or DockerMachine has been set, let's see if there is only one Docker Machine Running.");
            if (dockerMachineInstance.get().isDockerMachineInstalled()) {
                // we can inspect if docker machine has one and only one docker machine running, which means that would like to use that one
                Set<Machine> machines = this.dockerMachineInstance.get().list("state", "Running");

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

    private Map<String, String> resolveServerUriTcpProtocol(Map<String, String> cubeConfiguration) {

        if(cubeConfiguration.containsKey(CubeDockerConfiguration.DOCKER_URI)) {
            String dockerUri = cubeConfiguration.get(CubeDockerConfiguration.DOCKER_URI);
            if (dockerUri != null && dockerUri.startsWith("tcp://")) {
                if(containsCertPath(cubeConfiguration) || isDockerMachineNameSet() || containsDockerHostTag(dockerUri) ) {
                    cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, dockerUri.replace("tcp://", "https://"));
                } else {
                    cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, dockerUri.replace("tcp://", "http://"));
                }
            }
        }
        return cubeConfiguration;
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
