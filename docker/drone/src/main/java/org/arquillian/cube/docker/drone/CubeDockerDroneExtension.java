package org.arquillian.cube.docker.drone;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.drone.spi.Configurator;
import org.jboss.arquillian.drone.spi.Destructor;
import org.jboss.arquillian.drone.spi.Instantiator;
import org.jboss.arquillian.drone.webdriver.factory.ChromeDriverFactory;
import org.jboss.arquillian.drone.webdriver.factory.DockerRemoteWebDriverFactory;
import org.jboss.arquillian.drone.webdriver.factory.FirefoxDriverFactory;
import org.jboss.arquillian.drone.webdriver.factory.HtmlUnitDriverFactory;

/**
 * Cube Drone Extension register.
 */
public class CubeDockerDroneExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder extensionBuilder) {

        extensionBuilder.observer(InstallSeleniumCube.class);
        extensionBuilder.observer(DroneOverwriteConfigurator.class);
        extensionBuilder.observer(CubeDroneConfigurator.class);
        extensionBuilder.observer(VncRecorderLifecycleManager.class);

        overrideWebDriver(extensionBuilder, FirefoxDriverFactory.class);
        overrideWebDriver(extensionBuilder, ChromeDriverFactory.class);
        overrideWebDriver(extensionBuilder, HtmlUnitDriverFactory.class);

    }

    private void overrideWebDriver(ExtensionBuilder extensionBuilder, Class override) {
        extensionBuilder.override(Instantiator.class, override, DockerRemoteWebDriverFactory.class)
                        .override(Configurator.class, override, DockerRemoteWebDriverFactory.class)
                        .override(Destructor.class, override, DockerRemoteWebDriverFactory.class);
    }
}
