package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
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

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeDockerConfiguration> configurationProducer;

    @Inject
    private Instance<Boot2Docker> boot2DockerInstance;

    @Inject
    @ApplicationScoped
    private InstanceProducer<OperatingSystemFamily> operatingSystemFamilyInstanceProducer;

    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        operatingSystemFamilyInstanceProducer.set(new OperatingSystemResolver().currentOperatingSystem().getFamily());
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        config = resolveServerUriByOperativeSystem(config);
        config = resolveServerIp(config);
        CubeDockerConfiguration cubeConfiguration = CubeDockerConfiguration.fromMap(config);
        configurationProducer.set(cubeConfiguration);
    }

    private Map<String, String> resolveServerIp(Map<String, String> config) {
        String dockerServerUri = config.get(CubeDockerConfiguration.DOCKER_URI);
        if(dockerServerUri.contains(Boot2Docker.BOOT2DOCKER_TAG)) {
            dockerServerUri = resolveBoot2Docker(dockerServerUri, config.get(CubeDockerConfiguration.BOOT2DOCKER_PATH));
            config.put(CubeDockerConfiguration.DOCKER_URI, dockerServerUri);
            if(!config.containsKey(CubeDockerConfiguration.CERT_PATH)) {
                config.put(CubeDockerConfiguration.CERT_PATH, HomeResolverUtil.resolveHomeDirectoryChar(getDefaultTlsDirectory()));
            }
        }

        URI serverUri = URI.create(dockerServerUri);
        String serverIp = UNIX_SOCKET_SCHEME.equalsIgnoreCase(serverUri.getScheme()) ? "localhost" : serverUri.getHost();
        config.put(CubeDockerConfiguration.DOCKER_SERVER_IP, serverIp);
        return config;
    }

    private String resolveBoot2Docker(String tag,
            String boot2DockerPath) {
        return tag.replaceAll(Boot2Docker.BOOT2DOCKER_TAG, boot2DockerInstance.get().ip(boot2DockerPath, false));
    }

    private String getDefaultTlsDirectory() {
        return "~" + File.separator + ".boot2docker" + File.separator + "certs" + File.separator + "boot2docker-vm";
    }

    private Map<String, String> resolveServerUriByOperativeSystem(Map<String, String> cubeConfiguration) {
        if(!cubeConfiguration.containsKey(CubeDockerConfiguration.DOCKER_URI)) {
            String serverUri = operatingSystemFamilyInstanceProducer.get().getServerUri();
            cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, serverUri);
        }
        return cubeConfiguration;
    }
}
