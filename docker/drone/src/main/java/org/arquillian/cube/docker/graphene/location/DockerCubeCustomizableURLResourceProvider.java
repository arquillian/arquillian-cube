package org.arquillian.cube.docker.graphene.location;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import org.arquillian.cube.docker.drone.SeleniumContainers;
import org.arquillian.cube.docker.drone.util.IpAddressValidator;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.graphene.spi.configuration.GrapheneConfiguration;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * The DockerCubeCustomizableURLResourceProvider is used in the context of Graphene, if you use
 * the standalone framework integration option (see https://docs.jboss.org/author/display/ARQGRA2/Framework+Integration+Options)
 * with Docker Cube
 * and thus the Arquillian {@link org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider} is not
 * on
 * on the classpath, a way to inject the URL is required.
 * <p>
 * In this case the URL will be composed with next format:
 * <p>
 * <i>scheme</i> graphene configuration parameter [http, https, ...] or http if not set.
 * <p>
 * plus
 * <p>
 * <i>url</i> graphene configuration parameter. This can use the <i>dockerHost</i> special word which will be replaced at
 * runtime by docker host ip.
 * Also if <i>url</i> property starts with relative path, dockerHost resolution will be appended automatically at the
 * start of the <i>url</i>.
 * <p>
 * <p>
 * If <i>url</i> is <b>http://192.168.99.100/context</b> the result will be http://192.168.99.100/context
 * <p>
 * If <i>url</i> is <b>http://dockerHost/context</b> then the result will be http://&lt;ipOfDockerHost&gt;/context
 * <p>
 * If <i>url</i> is <b>http://&lt;containerName&gt;/context</b> so NOT dockerHost and not an IP then the result will be
 * http://&lt;internalIpOfGivenContainer&gt;/context
 * <p>
 * The next thing to resolve is the port of the URL.
 * <p>
 * If <i>url</i> has not port, then 80 port is used.
 * <p>
 * If <i>url</i> has a port (http://dockerHost:8080), Cube will use 8080 as exposed port.
 * <p>
 * For example <i>url</i> set to <b>http://mycontainer/context</b>, then the result will be
 * http://&lt;ipOfContainer&gt;:8080/context
 *
 * @see org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider
 **/
public class DockerCubeCustomizableURLResourceProvider implements ResourceProvider {

    private static final int NO_PORT = -1;

    @Inject
    Instance<GrapheneConfiguration> grapheneConfiguration;

    @Inject
    Instance<CubeDockerConfiguration> cubeDockerConfigurationInstance;

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return URL.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        try {
            return resolveUrl();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URL resolveUrl() throws MalformedURLException {
        final UrlBuilder urlBuilder = UrlBuilder.create();
        final GrapheneConfiguration grapheneConfiguration = this.grapheneConfiguration.get();

        if (grapheneConfiguration.getScheme() != null) {
            urlBuilder.protocol(grapheneConfiguration.getScheme());
        }

        final CubeDockerConfiguration cubeDockerConfiguration = cubeDockerConfigurationInstance.get();
        final String configuredUrl = grapheneConfiguration.getUrl();

        if (configuredUrl != null && !configuredUrl.isEmpty()) {

            String replacedWithDockerHostUrl = configuredUrl;

            // resolve dockerHost
            replacedWithDockerHostUrl = replacedWithDockerHostUrl
                .replace("dockerHost", cubeDockerConfiguration.getDockerServerIp());

            // We need to get the host part, port part and context
            URL currentUrl = new URL(replacedWithDockerHostUrl);
            String host = currentUrl.getHost();

            // check if host is an ip or not. If it is not an ip means that user wants to use internal ip of given container and host is the name of the container
            if (!IpAddressValidator.validate(host)) {
                host = getInternalIp(cubeDockerConfiguration, host);
            }

            urlBuilder.host(host);
            int port = currentUrl.getPort();

            if (port == NO_PORT) {
                port = 80;
            }

            urlBuilder.port(port);
            urlBuilder.context(currentUrl.getPath());
        } else {
            throw new IllegalArgumentException(
                "Arquillian Cube Graphene integration should provide a URL in Graphene extension configuration.");
        }

        try {
            return urlBuilder.build();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                "Configured custom URL from Graphene Configuration should be already a valid URL.");
        }
    }

    private String getInternalIp(CubeDockerConfiguration cubeDockerConfiguration, String containerName) {
        final Cube<?> cube = cubeRegistryInstance.get()
            .getCube(containerName);

        if (cube == null) {
            return cubeDockerConfiguration.getDockerServerIp();
        }

        if (cube.hasMetadata(HasPortBindings.class)) {
            return cube.getMetadata(HasPortBindings.class).getInternalIP();
        }

        return cubeDockerConfiguration.getDockerServerIp();
    }

    private static final class UrlBuilder {

        private String protocol = "http";
        private String host;
        private int port = -1;
        private String file = "";

        private UrlBuilder() {
            super();
        }

        public static UrlBuilder create() {
            return new UrlBuilder();
        }

        public UrlBuilder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public UrlBuilder host(String host) {
            this.host = host;
            return this;
        }

        public UrlBuilder port(int port) {
            this.port = port;
            return this;
        }

        public UrlBuilder context(String context) {
            this.file = context;
            return this;
        }

        public URL build() throws MalformedURLException {

            if (protocol == null || protocol.isEmpty()) {
                throw new IllegalArgumentException("Protocol cannot be null or empty");
            }

            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Host cannot be null or empty");
            }

            if (port < 0) {
                throw new IllegalArgumentException("Port cannot be negative");
            }

            return new URL(protocol, host, port, file);
        }
    }
}
