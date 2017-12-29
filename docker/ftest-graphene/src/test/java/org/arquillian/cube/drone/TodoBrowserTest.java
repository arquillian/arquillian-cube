package org.arquillian.cube.drone;

import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.InitialPage;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

@Category(RequiresDockerMachine.class)
@RequiresDockerMachine(name = "dev")
@RunWith(ArquillianConditionalRunner.class)
public class TodoBrowserTest {

    @Drone
    WebDriver webDriver;

    @Test
    public void shouldShowHelloWorld(@InitialPage HomePage homePage) {
        homePage.assertOnWelcomePage();
    }
}
