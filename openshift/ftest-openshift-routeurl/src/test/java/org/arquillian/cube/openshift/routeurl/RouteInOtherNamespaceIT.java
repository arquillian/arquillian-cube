package org.arquillian.cube.openshift.routeurl;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class RouteInOtherNamespaceIT {

    @RouteURL(value = "docker-registry", namespace = "default")
    private URL routeURL;

    @Test
    public void injectedIntoClassShouldNotBeNull() {
        assertThat(routeURL).isNotNull();
    }

    @Test
    public void injectedIntoMethodShouldNotBeNull(
        @RouteURL(value = "docker-registry", namespace = "default") URL routeURL) {
        assertThat(routeURL).isNotNull();
    }

}
