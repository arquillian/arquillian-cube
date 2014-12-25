package org.arquillian.cube.impl.containerless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CubeControlEvent;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class ContainerlessDockerDeployableContainer implements DeployableContainer<ContainerlessConfiguration> {

    private static final String DOCKERFILE_TEMPLATE = "DockerfileTemplate";

    private ContainerlessConfiguration configuration;

    @Inject
    private Instance<CubeRegistry> cubeRegistryInstance;

    @Inject
    private Event<CubeControlEvent> controlEvent;

    @Override
    public Class<ContainerlessConfiguration> getConfigurationClass() {
        return ContainerlessConfiguration.class;
    }

    @Override
    public void setup(ContainerlessConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        // should be done at deployment time.
    }

    @Override
    public void stop() throws LifecycleException {
        // should be done at undeployment time.
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String containerlessDocker = this.configuration.getContainerlessDocker();
        final CubeRegistry cubeRegistry = cubeRegistryInstance.get();

        Cube cube = cubeRegistry.getCube(containerlessDocker);
        if (cube == null) {
            //Is there a way to ignore it? Or we should throw an exception?
            throw new IllegalArgumentException("No Containerless Docker container configured in extension with id "+containerlessDocker);
        }
        Map<String, Object> cubeConfiguration = cube.configuration();

        if(cubeConfiguration.containsKey("buildImage")) {
            Map<String, Object> params = asMap(cubeConfiguration, "buildImage");
            if(params.containsKey("dockerfileLocation")) {
                File location = new File((String) params.get("dockerfileLocation"));
                if(location.isDirectory()) {
                    File templateDockerfile = new File(location, DOCKERFILE_TEMPLATE);
                    try {
                        String deployableFilename = archive.getName();
                        Map<String, String> values = new HashMap<String, String>();
                        values.put("deployableFilename", deployableFilename);
                        String templateContent = IOUtil.asString(new FileInputStream(templateDockerfile));
                        String dockerfileContent = IOUtil.replacePlaceholders(templateContent, values);
                        File dockerfile = new File(location, "Dockerfile");
                        if(dockerfile.exists()) {
                            //log that the file already exists and it is going to be renamed.
                        }
                        dockerfile.deleteOnExit();
                        IOUtil.toFile(dockerfileContent, dockerfile);
                        File deployableOutputFile = new File(location, deployableFilename);
                        deployableOutputFile.deleteOnExit();

                        archive.as(ZipExporter.class).exportTo(deployableOutputFile, true);

                        //fire events
                        
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException("Containerless Docker container requires a file named "+ DOCKERFILE_TEMPLATE);
                    }
                } else {
                    throw new IllegalArgumentException("Dockerfile Template of containerless Docker container must be in a directory.");
                }
            } else {
                throw new IllegalArgumentException("Containerless Docker container should be built in Dockerfile, and dockerfileLocation property not found.");
            }
        } else {
            throw new IllegalArgumentException("Containerless Docker container should be built in Dockerfile, and buildImage property not found.");
        }
        return null;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @SuppressWarnings("unchecked")
    private static final Map<String, Object> asMap(Map<String, Object> map, String property) {
        return (Map<String, Object>) map.get(property);
    }
}
