package org.arquillian.cube.openshift.ftest;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.InitialPage;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class TodoBrowserIT {

    @Drone
    WebDriver webDriver;

    @Test
    public void shouldShowHelloWorld(@InitialPage HomePage homePage) {
        homePage.assertOnWelcomePage();
    }
}
