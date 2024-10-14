package org.arquillian.cube.docker.restassured;

import io.restassured.builder.RequestSpecBuilder;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.DockerUrl;
import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class RequestSpecBuilderResourceProvider implements ResourceProvider {

    private static final Logger logger = Logger.getLogger(RequestSpecBuilderResourceProvider.class.getName());

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;

    @Inject
    Instance<HostIpContext> hostUriContext;

    @Inject
    private Instance<RequestSpecBuilder> requestSpecBuilderInstance;

    @Override
    public boolean canProvide(Class<?> type) {
        return RequestSpecBuilder.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {

        RequestSpecBuilder requestSpecBuilder = requestSpecBuilderInstance.get();

        if (requestSpecBuilder == null) {
            throw new IllegalStateException("RequestSpecBuilder was not found.");
        }

        //Override with elements provided in DockerUrl
        final Optional<DockerUrl> dockerUrlAnnotation = getDockerUrlAnnotation(qualifiers);

        if (dockerUrlAnnotation.isPresent()) {
            final DockerUrl dockerUrl = dockerUrlAnnotation.get();

            //We only override in case of setting here as https, in other cases, the original value is used which correctly
            // points to docker host.
            if ("https".equals(dockerUrl.protocol())) {
                requestSpecBuilder.setBaseUri(dockerUrl.protocol() + "://" + getHost());
            }

            final String containerName = dockerUrl.containerName();

            final int exposedPort = dockerUrl.exposedPort();
            final int bindPort = getBindingPort(containerName, exposedPort);

            if (bindPort > 0) {
                requestSpecBuilder.setPort(bindPort);
            } else {
                logger.log(Level.WARNING, String.format("There is no container with id %s.", containerName));
            }

            requestSpecBuilder.setBasePath(dockerUrl.context());
        }

        return requestSpecBuilder;
    }

    private Optional<DockerUrl> getDockerUrlAnnotation(Annotation[] annotations) {

        return Arrays.stream(annotations)
            .filter(annotation -> DockerUrl.class.equals(annotation.annotationType()))
            .map(annotation -> (DockerUrl) annotation)
            .findFirst();
    }

    String getHost() {
        final HostIpContext hostIpContext = hostUriContext.get();
        return hostIpContext.getHost();
    }

    private int getBindingPort(String cubeId, int exposedPort) {

        int bindPort = -1;

        final Cube cube = getCube(cubeId);

        if (cube != null) {
            final HasPortBindings portBindings = (HasPortBindings) cube.getMetadata(HasPortBindings.class);
            final HasPortBindings.PortAddress mappedAddress = portBindings.getMappedAddress(exposedPort);

            if (mappedAddress != null) {
                bindPort = mappedAddress.getPort();
            }
        }

        return bindPort;
    }

    private Cube getCube(String cubeId) {
        return cubeRegistryInstance.get().getCube(cubeId);
    }
}
