package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.arquillian.cube.docker.impl.util.*;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerConfigurator {

    private static final String EXTENSION_NAME = "docker";
    private static final String UNIX_SOCKET_SCHEME = "unix";
    public static final String DOCKER_HOST = "DOCKER_HOST";
    private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";
    private static final String DOCKER_MACHINE_NAME = "DOCKER_MACHINE_NAME";

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeDockerConfiguration> configurationProducer;

    @Inject
    private Instance<Boot2Docker> boot2DockerInstance;

    @Inject
    private Instance<DockerMachine> dockerMachineInstance;

    @Inject
    @ApplicationScoped
    private InstanceProducer<OperatingSystemFamily> operatingSystemFamilyInstanceProducer;

    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        operatingSystemFamilyInstanceProducer.set(new OperatingSystemResolver().currentOperatingSystem().getFamily());
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        config = resolveSystemEnvironmentVariables(config);
        config = resolveServerUriByOperativeSystem(config);
        config = resolveServerIp(config);
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(config);
        configurationProducer.set(cubeConfiguration);
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

        if (dockerServerUri.contains(AbstractCliInternetAddressResolver.DOCKERHOST_TAG)) {
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
