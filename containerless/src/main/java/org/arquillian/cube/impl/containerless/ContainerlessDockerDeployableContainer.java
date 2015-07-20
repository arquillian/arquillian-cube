package org.arquillian.cube.impl.containerless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.CubeControlEvent;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.exporter.TarExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class ContainerlessDockerDeployableContainer implements DeployableContainer<ContainerlessConfiguration> {

    private static final String DOCKERFILE_TEMPLATE = "DockerfileTemplate";
    private static final Logger log = Logger.getLogger(ContainerlessDockerDeployableContainer.class.getName());
    
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
        final CubeRegistry cubeRegistry = cubeRegistryInstance.get();

        Cube cube = resolveMainCube(cubeRegistry);

        Map<String, Object> cubeConfiguration = cube.configuration();
        //Only works with images using a Dockerfile.
        if (cubeConfiguration.containsKey("buildImage")) {
            Map<String, Object> params = asMap(cubeConfiguration, "buildImage");
            if (params.containsKey("dockerfileLocation")) {
                File location = new File((String) params.get("dockerfileLocation"));
                if (location.isDirectory()) {
                    //Because ShrinkWrap may create different jar files depending on what we are testing in this case
                    //we need a template which is the responsible to copy the jar to desired location
                    try {
                        createDockerfileFromTemplate(archive, location);
                        // fire events as usually.
                        controlEvent.fire(new CreateCube(cube));
                        controlEvent.fire(new StartCube(cube));
                        return createProtocolMetadata(cube, archive);
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException("Containerless Docker container requires a file named "
                                + DOCKERFILE_TEMPLATE);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Dockerfile Template of containerless Docker container must be in a directory.");
                }
            } else {
                throw new IllegalArgumentException(
                        "Containerless Docker container should be built in Dockerfile, and dockerfileLocation property not found.");
            }
        } else {
            throw new IllegalArgumentException(
                    "Containerless Docker container should be built in Dockerfile, and buildImage property not found.");
        }
    }

    private Cube resolveMainCube(CubeRegistry cubeRegistry) {
        Cube cube = null;
        if(this.configuration.isContainerlessDockerSet()) {
            String containerlessDocker = this.configuration.getContainerlessDocker();
            cube = cubeRegistry.getCube(containerlessDocker);
            if (cube == null) {
                // Is there a way to ignore it? Or we should throw an exception?
                throw new IllegalArgumentException("No Containerless Docker container configured in extension with id "
                        + containerlessDocker);
            }
        } else {
            List<Cube> cubes = cubeRegistry.getCubes();
            if(cubes.size() == 1) {
                cube = cubes.get(0);
            } else {
                throw new IllegalArgumentException("More than one container eligible for being the main instance. Use containerlessDocker property to set one.");
            }
        }
        return cube;
    }

    private void createDockerfileFromTemplate(Archive<?> archive, File location)
            throws FileNotFoundException {
        File templateDockerfile = new File(location, DOCKERFILE_TEMPLATE);
        String deployableFilename = archive.getName();
        Map<String, String> values = new HashMap<String, String>();
        values.put("deployableFilename", deployableFilename);
        String templateContent = IOUtil.asStringPreservingNewLines(new FileInputStream(templateDockerfile));
        //But because deployable file is created by shrinkwrap we need to replace the deploy file name to the one created.
        String dockerfileContent = IOUtil.replacePlaceholders(templateContent, values);
        File dockerfile = new File(location, "Dockerfile");
        if (dockerfile.exists()) {
            log.fine("Dockerfile file is already found in current build directory and is going to be renamed to Dockerfile.old.");
            dockerfile.renameTo(new File(location, "Dockerfile.old"));
            dockerfile = new File(location, "Dockerfile");
        }
        dockerfile.deleteOnExit();
        //The content is written to real Dockerfile which will be used during built time.
        IOUtil.toFile(dockerfileContent, dockerfile);
        File deployableOutputFile = new File(location, deployableFilename);
        deployableOutputFile.deleteOnExit();
        //file is saved to Dockerfile directory so can be copied inside image.
        if(archive instanceof GenericArchive) {
            //In case of generic archives a tgz exporter should be used so Docker can uncompress it automatically.
            archive.as(TarExporter.class).exportTo(deployableOutputFile, true);
        } else {
            archive.as(ZipExporter.class).exportTo(deployableOutputFile, true);
        }
    }

    private ProtocolMetaData createProtocolMetadata(Cube cube, Archive<?> deployment) {
        Binding bindings = cube.bindings();
        //ProtocolMetadataUpdater will reajust the port to the exposed ones.

        HTTPContext httpContext = null;
        if(this.configuration.isEmbeddedPortSet()) {
            httpContext = new HTTPContext(bindings.getIP(), this.configuration.getEmbeddedPort());
        } else {
            if(bindings.getNumberOfPortBindings() == 1) {
                httpContext = new HTTPContext(bindings.getIP(), bindings.getFirstPortBinding().getBindingPort());
            } else {
                throw new IllegalArgumentException("More than one port binding eligible. Set one using embeddedPort property");
            }
        }

        // TEMP HACK to allow in-container testing without communicating with the Server mgm api
        if(containsArquillianServletProtocol(deployment)) {
            addArquillianTestServlet(deployment, httpContext);
        }
        return new ProtocolMetaData().addContext(httpContext);
    }

    private boolean containsArquillianServletProtocol(Archive<?> deployment) {
        return deployment.getContent(Filters.include(".*arquillian-protocol.jar")).size() > 0;
	}

	private void addArquillianTestServlet(Archive<?> deployment, HTTPContext httpContext) {
        httpContext.add(new Servlet("ArquillianServletRunner", extractContextName(deployment)));
	}

	private String extractContextName(Archive<?> deployment) {
        String name = deployment.getName();
        name = name.substring(0, name.lastIndexOf("."));
        return name;
	}

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        String containerlessDocker = this.configuration.getContainerlessDocker();
        final CubeRegistry cubeRegistry = cubeRegistryInstance.get();

        Cube cube = cubeRegistry.getCube(containerlessDocker);
        if (cube != null) {
            controlEvent.fire(new StopCube(cube));
            controlEvent.fire(new DestroyCube(cube));
        }
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
