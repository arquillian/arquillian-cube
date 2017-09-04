package org.arquillian.cube.docker.drone;

import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SeleniumContainersTest {

    private final static Matcher<PortBinding> SELENIUM_PORT_BINDING_MATCHER =
            new CustomMatcher<PortBinding>("a port binding for Selenium exposed port"){
                @Override
                public boolean matches(Object item){
                    if (item instanceof PortBinding) {
                        PortBinding portBinding = (PortBinding) item;
                        return portBinding.getExposedPort().getExposed() == SeleniumContainers.SELENIUM_EXPOSED_PORT 
                                && portBinding.getBound() >= 49152 && portBinding.getBound() < 65535;
                    }
                    return false;
                }
            };
    
    @Mock
    CubeDroneConfiguration cubeDroneConfiguration;

    @Test
    public void shouldCreateCustomContainerFromImageName() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserImage()).thenReturn("mycompany/mybrowser:1.0");

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("mycompany/mybrowser:1.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(SELENIUM_PORT_BINDING_MATCHER));
    }

    @Test
    public void shouldCreateCustomContainerFromDockerfile() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(true);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);
        when(cubeDroneConfiguration.getBrowserDockerfileLocation()).thenReturn("src/test/resources/browser");

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getBuildImage().getDockerfileLocation().toString(),
            is("src/test/resources/browser"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(SELENIUM_PORT_BINDING_MATCHER));
    }

    @Test
    public void shouldTakePrecedenceDockerfileDirectoryThanImage() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(true);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserDockerfileLocation()).thenReturn("src/test/resources/browser");

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getBuildImage().getDockerfileLocation().toString(),
            is("src/test/resources/browser"));
        assertThat(firefox.getSeleniumContainer().getImage(), is(nullValue()));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(SELENIUM_PORT_BINDING_MATCHER));
    }

    @Test
    public void shouldCreateContainerForFirefox() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-firefox-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(SELENIUM_PORT_BINDING_MATCHER));
        assertThat(firefox.getSeleniumContainer().getAwait().getResponseCode(), is(403));
    }

    @Test
    public void shouldCreateContainerForChrome() {

        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);

        final SeleniumContainers firefox = SeleniumContainers.create("chrome", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("chrome"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-chrome-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(SELENIUM_PORT_BINDING_MATCHER));
        assertThat(firefox.getSeleniumContainer().getAwait().getResponseCode(), is(403));
    }
}
