package org.arquillian.cube.docker.graphene.location;

import org.arquillian.cube.docker.drone.SeleniumContainers;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.util.SinglePortBindResolver;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.graphene.spi.configuration.GrapheneConfiguration;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The DockerCubeCustomizableURLResourceProvider is used in the context of Graphene, if you use
 * the standalone framework integration option (see https://docs.jboss.org/author/display/ARQGRA2/Framework+Integration+Options) with Docekr Cube
 * and thus the Arquillian {@link org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider} is not on
 * on the classpath, a way to inject the URL is required.
 *
 * In this case the URL will be composed with next format:
 *
 * <i>scheme</i> graphene configuration parameter [http, https, ...] or http if not set.
 *
 * plus
 *
 * <i>url</i> graphene configuration parameter. This can use the <i>dockerHost</i> special word which will be replaced at runtime by docker host ip.
 * Also if <i>url</i> property starts with relative path, dockerHost resolution will be appended automatically at the start of the <i>url</i>.
 *
 * For example an empty or not present <i>scheme</i> and <i>url</i> will result in http://&lt;ipOfDockerhost&gt;
 *
 * If <i>url</i> is <b>/192.168.99.100/context</b> the result will be http://192.168.99.100/context
 *
 * If <i>url</i> is <b>context</b> then the result will be http://&lt;ipOfDockerHost&gt;/context
 *
 * If <i>url</i> is <b>/dockerHost/context</b> then the result will be http://&lt;ipOfDockerHost&gt;/context
 *
 * If <i>url</i> is <b>dockerHost/context</b> then the result will be http://&lt;ipOfDockerHost&gt;/context
 *
 * The next thing to resolve is the port of the URL.
 *
 * If <i>url</i> has no port, Cube will find among all cubes if there is only one bounded port. If it is the case this is the one used, if not 8080 is used.
 *
 * If <i>url</i> has a port (dockerHost:8080), Cube will find a service which <b>exposes</b> port 8080 with a bind port too and will resolve to bind port (port of docker host).
 * If there is no exposed port will assume that it is directly the binding port. Notice that this latter case affects the portability of the test.
 * If there is more than one cube with given exposed port with <b>port binding</b> and exception is thrown.
 *
 * For example having a service with 9090:8080 port configuration and <i>url</i> set to /dockerHost:8080/context, then the result will be http://&lt;ipOfDockerHost&gt;:9090/context
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
    Instance<SeleniumContainers> seleniumContainersInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return URL.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return resolveUrl();
    }

    private URL resolveUrl() {
        final UrlBuilder urlBuilder = UrlBuilder.create();
        final GrapheneConfiguration grapheneConfiguration = this.grapheneConfiguration.get();

        if (grapheneConfiguration.getScheme() != null) {
            urlBuilder.protocol(grapheneConfiguration.getScheme());
        }

        final CubeDockerConfiguration cubeDockerConfiguration = cubeDockerConfigurationInstance.get();
        final String configuredUrl = grapheneConfiguration.getUrl();

        if (configuredUrl != null && !configuredUrl.isEmpty()) {

            if (isAnAbsoluteUrl(configuredUrl) || startWithDockerHost(configuredUrl)) {

                String replacedWithDockerHostUrl = configuredUrl;

                // remove initial slash
                if (isAnAbsoluteUrl(configuredUrl)) {
                    replacedWithDockerHostUrl = configuredUrl.substring(1);
                }

                // resolve dockerHost
                replacedWithDockerHostUrl = replacedWithDockerHostUrl
                        .replace("dockerHost", cubeDockerConfiguration.getDockerServerIp());

                // We need to get the host part, port part and context
                urlBuilder.host(resolveHost(replacedWithDockerHostUrl));
                urlBuilder.port(resolvePort(replacedWithDockerHostUrl));
                urlBuilder.context(resolveContext(replacedWithDockerHostUrl));

            } else {
                urlBuilder.host(cubeDockerConfiguration.getDockerServerIp());
                urlBuilder.port(resolveBindPort(NO_PORT));
                urlBuilder.context(configuredUrl);
            }

        } else {
            urlBuilder.host(cubeDockerConfiguration.getDockerServerIp());
            urlBuilder.port(resolveBindPort(NO_PORT));
        }

        try {
            return urlBuilder.build();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Configured custom URL from GrapheneConfiguration should be already a valid URL.");
        }
    }

    private String resolveContext(String url) {
        int indexOfSlash = url.indexOf('/');

        if (indexOfSlash > 0) {
            return url.substring(indexOfSlash + 1);
        }

        return "";
    }

    private int resolvePort(String url) {
        int port = extractPort(url);
        return resolveBindPort(port);
    }

    private int resolveBindPort(int port) {
        final CubeDockerConfiguration cubeDockerConfiguration = cubeDockerConfigurationInstance.get();

        final SeleniumContainers seleniumContainers = seleniumContainersInstance.get();
        if (port == NO_PORT) {
            return SinglePortBindResolver.resolveBindPort(cubeDockerConfiguration,
                    seleniumContainers.getSeleniumContainerName(),
                    seleniumContainers.getVncContainerName());

        } else {
            return SinglePortBindResolver.resolveBindPort(cubeDockerConfiguration, port,
                    seleniumContainers.getSeleniumContainerName(),
                    seleniumContainers.getVncContainerName());
        }
    }

    private int extractPort(String url) {
        int colonLocation = url.indexOf(':');

        if (colonLocation > 0) {
            for (int i = colonLocation +1; i < url.length(); i++) {
                if (url.charAt(i) == '/') {
                    return Integer.parseInt(url.substring(colonLocation + 1, i));
                }
            }
            return Integer.parseInt(url.substring(colonLocation + 1));
        }

        return NO_PORT;
    }

    private String resolveHost(String url) {
        for (int i=0; i < url.length(); i++) {
            if (url.charAt(i) == ':' || url.charAt(i) == '/') {
                return url.substring(0, i);
            }
        }

        return url;
    }


    private boolean startWithDockerHost(String url) {
        return url.startsWith("dockerHost");
    }

    private boolean isAnAbsoluteUrl(String url) {
        return url.startsWith("/");
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
