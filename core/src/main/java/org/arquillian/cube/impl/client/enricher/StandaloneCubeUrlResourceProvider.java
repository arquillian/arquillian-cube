package org.arquillian.cube.impl.client.enricher;

import org.arquillian.cube.DockerUrl;
import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StandaloneCubeUrlResourceProvider implements ResourceProvider {

   @Inject
   Instance<HostIpContext> hostUriContext;

   private static final Logger logger = Logger.getLogger(StandaloneCubeUrlResourceProvider.class.getName());

   @Inject
   Instance<CubeRegistry> cubeRegistryInstance;

   @Override
   public boolean canProvide(Class<?> aClass) {
      return URL.class.isAssignableFrom(aClass);
   }

   @Override
   public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {

      final Optional<DockerUrl> optionalDockerUrlAnnotation = getDockerUrlAnnotation(annotations);
      if (optionalDockerUrlAnnotation.isPresent()) {

         final String host = getHost();

         final DockerUrl dockerUrl = optionalDockerUrlAnnotation.get();
         final String containerName = dockerUrl.containerName();

         final int exposedPort = dockerUrl.exposedPort();
         final int bindPort = getBindingPort(containerName, exposedPort);

         if (bindPort > 0) {
            return createUrl(host, dockerUrl, bindPort);
         } else {
            logger.log(Level.WARNING, String.format("There is no container with id %s.", containerName));
         }

      }
      return null;
   }

   Object createUrl(String host, DockerUrl dockerUrl, int bindPort) {
      try {
         return new URL(dockerUrl.protocol(), host, bindPort, dockerUrl.context());
      } catch (MalformedURLException e) {
         throw new IllegalArgumentException(e);
      }
   }

   String getHost() {
      final HostIpContext hostIpContext = hostUriContext.get();
      return hostIpContext.getHost();
   }

   private Optional<DockerUrl> getDockerUrlAnnotation(Annotation[] annotations) {

      return Arrays.stream(annotations)
              .filter(annotation -> DockerUrl.class.equals(annotation.annotationType()))
              .map(annotation -> (DockerUrl) annotation)
              .findFirst();

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
