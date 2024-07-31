package org.arquillian.cube.docker.impl.client;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InfoCmd;
import com.github.dockerjava.api.model.Info;
import org.arquillian.cube.docker.impl.util.AbstractCliInternetAddressResolver;
import org.arquillian.cube.docker.impl.util.DefaultDocker;
import org.arquillian.cube.docker.impl.util.GitHubUtil;
import org.arquillian.cube.docker.impl.util.HomeResolverUtil;
import org.arquillian.cube.docker.impl.util.Machine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemInterface;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.spacelift.Spacelift;
import org.arquillian.spacelift.task.net.DownloadTool;

import javax.ws.rs.ProcessingException;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
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
    private final DefaultDocker defaultDocker;
    private final OperatingSystemInterface operatingSystem;

    public CubeDockerConfigurationResolver(Top top,
                                           DefaultDocker defaultDocker, OperatingSystemInterface operatingSystem) {
        this.top = top;
        this.operatingSystem = operatingSystem;
        this.defaultDocker = defaultDocker;
    }

    /**
     * Resolves the configuration.
     *
     * @param config The specified configuration.
     * @return The resolved configuration.
     */
    public Map<String, String> resolve(Map<String, String> config) {
        config = resolveSystemEnvironmentVariables(config);
        config = resolveSystemDefaultSetup(config);
        config = resolveDockerInsideDocker(config);
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

        if (!config.containsKey(CubeDockerConfiguration.TLS_VERIFY) && isDockerTlsVerifySet()) {
            config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.TRUE.toString());
        }

        return config;
    }
    
    private Map<String, String> resolveSystemDefaultSetup(Map<String, String> config) {
        if (!config.containsKey(CubeDockerConfiguration.DOCKER_URI)) {
            final String defaultUri = operatingSystem.getDefaultFamily().getServerUri();
            URI uri = URI.create(defaultUri);
            if (Files.exists(FileSystems.getDefault().getPath(uri.getPath()))){

                DockerClient client = defaultDocker.getDefaultDockerClient(defaultUri);
                InfoCmd cmd = client.infoCmd();
                try {
                    Info info = cmd.exec();
                    config.put(CubeDockerConfiguration.DOCKER_URI, defaultUri);
                    log.info(String.format("Connected to docker (%s) using default settings version: %s kernel: %s",
                        info.getName(), info.getServerVersion(), info.getKernelVersion()));
                } catch (ProcessingException e){
                    log.info(String.format("Could not connect to default socket %s. Go on with ",
                        operatingSystem.getDefaultFamily().getServerUri()));
                }

            }
        }

        return config;
    }

    private Map<String, String> resolveServerIp(Map<String, String> config) {
        String dockerServerUri = config.get(CubeDockerConfiguration.DOCKER_URI);

        if (containsDockerHostTag(dockerServerUri)) {

            if (!config.containsKey(CubeDockerConfiguration.TLS_VERIFY)) {
                config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(true));
            }

        }

        config.put(CubeDockerConfiguration.DOCKER_URI, dockerServerUri);

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

        if (scheme.equals("unix") || scheme.equals("npipe")){
            config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(false));
        }

        if (!config.containsKey(CubeDockerConfiguration.TLS_VERIFY)) {
            config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(true));

            if (operatingSystem.getFamily() == OperatingSystemFamily.LINUX) {

                String dockerServerIp = config.get(CubeDockerConfiguration.DOCKER_SERVER_IP);

                config.put(CubeDockerConfiguration.TLS_VERIFY, Boolean.toString(false));

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



    private boolean containsCertPath(Map<String, String> cubeConfiguration) {
        return cubeConfiguration.containsKey(CubeDockerConfiguration.CERT_PATH);
    }

    private Map<String, String> resolveServerUriByOperativeSystem(Map<String, String> cubeConfiguration) {
        if (!cubeConfiguration.containsKey(CubeDockerConfiguration.DOCKER_URI)) {
            String serverUri = operatingSystem.getFamily().getServerUri();
            cubeConfiguration.put(CubeDockerConfiguration.DOCKER_URI, serverUri);
        }
        return cubeConfiguration;
    }
}
