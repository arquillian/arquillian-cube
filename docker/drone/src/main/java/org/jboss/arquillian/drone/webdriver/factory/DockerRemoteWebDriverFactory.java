package org.jboss.arquillian.drone.webdriver.factory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.docker.drone.SeleniumContainers;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.drone.spi.Configurator;
import org.jboss.arquillian.drone.spi.Destructor;
import org.jboss.arquillian.drone.spi.Instantiator;
import org.jboss.arquillian.drone.webdriver.configuration.WebDriverConfiguration;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Browser;
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
        RemoteWebDriver remoteWebDriver = new RemoteWebDriver(createSeleniumUrl(), getDesiredCapabilities());
        return remoteWebDriver;
    }

    private DesiredCapabilities getDesiredCapabilities() {

        final SeleniumContainers seleniumContainers = seleniumContainersInstance.get();
        switch (seleniumContainers.getBrowser()) {
            case "firefox":
                return new DesiredCapabilities(Browser.FIREFOX.browserName(), null, null);
            case "chrome":
                return new DesiredCapabilities(Browser.CHROME.browserName(), null, null);
            // Never should happen since it is protected inside selenium containers class
            default:
                return new DesiredCapabilities(Browser.FIREFOX.browserName(), null, null);
        }
    }

    private URL createSeleniumUrl() {
        try {
            final CubeDockerConfiguration cubeDockerConfiguration = cubeDockerConfigurationInstance.get();
            final SeleniumContainers seleniumContainers = seleniumContainersInstance.get();
            final String dockerServerIp = cubeDockerConfiguration.getDockerServerIp();
            return new URL("http", dockerServerIp, seleniumContainers.getSeleniumBoundedPort(), "/wd/hub");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
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
