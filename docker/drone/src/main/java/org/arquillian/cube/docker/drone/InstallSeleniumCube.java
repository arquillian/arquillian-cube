package org.arquillian.cube.docker.drone;

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.Map;

/**
 * Class that modifies the docker composition to add Selenium docker container and if configured the VNC client container too.
 */
public class InstallSeleniumCube {

    private static final String DEFAULT_BROWSER = "firefox";

    @Inject
    @ApplicationScoped
    InstanceProducer<SeleniumContainers> seleniumContainersInstanceProducer;

    @Inject
    Instance<CubeDroneConfiguration> cubeDroneConfigurationInstance;

    // ten less than Cube Q
    public void install(@Observes(precedence = 90) CubeDockerConfiguration configuration, ArquillianDescriptor arquillianDescriptor) {

        DockerCompositions cubes = configuration.getDockerContainersContent();

        final SeleniumContainers seleniumContainers = SeleniumContainers.create(getBrowser(arquillianDescriptor));
        cubes.add(seleniumContainers.getSeleniumContainerName(), seleniumContainers.getSeleniumContainer());

        final boolean recording = cubeDroneConfigurationInstance.get().isRecording();
        if (recording) {
            cubes.add(seleniumContainers.getVncContainerName(), seleniumContainers.getVncContainer());
        }

        seleniumContainersInstanceProducer.set(seleniumContainers);

        System.out.println("SELENIUM INSTALLED");
        System.out.println(ConfigUtil.dump(cubes));
    }

    private String getBrowser(ArquillianDescriptor arquillianDescriptor) {
        final ExtensionDef extension = arquillianDescriptor.extension("webdriver");

        if (extension == null) {
            return DEFAULT_BROWSER;
        }

        final Map<String, String> extensionProperties = extension.getExtensionProperties();

        if (extensionProperties == null) {
            return DEFAULT_BROWSER;
        }

        if (extensionProperties.containsKey("browser")) {
            return extensionProperties.get("browser");
        } else {
            return DEFAULT_BROWSER;
        }

    }
}
