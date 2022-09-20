package org.jboss.arquillian.drone.webdriver.factory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.docker.drone.SeleniumContainers;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.drone.spi.Configurator;
import org.jboss.arquillian.drone.spi.Destructor;
import org.jboss.arquillian.drone.spi.Instantiator;
import org.jboss.arquillian.drone.webdriver.configuration.WebDriverConfiguration;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Custom Remote WebDriver Factory that generates a RemoteWebDriver pointing to Docker Selenium Docker IP.
 */
public class DockerRemoteWebDriverFactory extends AbstractWebDriverFactory<RemoteWebDriver> implements
    Configurator<RemoteWebDriver, WebDriverConfiguration>, Instantiator<RemoteWebDriver, WebDriverConfiguration>,
    Destructor<RemoteWebDriver> {

    private static final Logger log = Logger.getLogger(DockerRemoteWebDriverFactory.class.getName());

    @Inject
    Instance<CubeDockerConfiguration> cubeDockerConfigurationInstance;

    @Inject
    Instance<SeleniumContainers> seleniumContainersInstance;

    @Inject
    @ApplicationScoped
    private InstanceProducer<DockerClientExecutor> dockerClientExecutorProducer;

    @Override
    public void destroyInstance(RemoteWebDriver remoteWebDriver) {
        try {
            remoteWebDriver.quit();
        } catch (WebDriverException e) {
            log.log(Level.WARNING, "@Drone {0} has been already destroyed and can't be destroyed again.",
                remoteWebDriver.getClass()
                    .getSimpleName());
        }
    }

    @Override
    public RemoteWebDriver createInstance(WebDriverConfiguration webDriverConfiguration) {
        return new RemoteWebDriver(createSeleniumUrl(), getDesiredCapabilities());
    }

    private DesiredCapabilities getDesiredCapabilities() {

        final SeleniumContainers seleniumContainers = seleniumContainersInstance.get();
        switch (seleniumContainers.getBrowser()) {
            case "firefox":
                return DesiredCapabilities.firefox();
            case "chrome":
                return DesiredCapabilities.chrome();
            // Never should happen since it is protected inside selenium containers class
            default:
                log.log(Level.WARNING, "Using Firefox as fallback browser");
                return DesiredCapabilities.firefox();
        }
    }

    private URL createSeleniumUrl() {

        final SeleniumContainers seleniumContainers = seleniumContainersInstance.get();
        final String containerName = seleniumContainers.getSeleniumContainerName();

        final CubeDockerConfiguration cubeDockerConfiguration = cubeDockerConfigurationInstance.get();
        final CubeContainer browser = cubeDockerConfiguration.getDockerContainersContent().get(containerName);
        final String ip = (cubeDockerConfiguration.isDockerInsideDockerResolution()
            ? cubeDockerConfiguration.getDockerServerIp() : URI.create(cubeDockerConfiguration.getDockerServerUri()).getHost());

        try {
            final URL url = new URL("http", ip, this.getSeleniumPort(browser), "/wd/hub");
            log.log(Level.INFO, "Using Selenium server network address: " + url.toExternalForm());
            return url;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private int getSeleniumPort(final CubeContainer browser) {
        return browser.getPortBindings()
            .stream()
            .filter(p -> (p.getBound() != 5900)) //VNC
            .findFirst()
            .orElse(PortBinding.valueOf("14444->4444")).getBound();
    }

    @Override
    protected String getDriverReadableName() {
        return "Docker Remote Web Driver";
    }

    @Override
    public int getPrecedence() {
        return Integer.MAX_VALUE;
    }
}
