package org.arquillian.cube.docker.drone;

import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SeleniumContainersTest {


    @Test
    public void shouldCreateContainerForFirefox() {
        final SeleniumContainers firefox = SeleniumContainers.create("firefox");
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-firefox-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
    }

    @Test
    public void shouldCreateContainerForChrome() {
        final SeleniumContainers firefox = SeleniumContainers.create("chrome");
        assertThat(firefox.getBrowser(), is("chrome"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-chrome-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
    }

}
