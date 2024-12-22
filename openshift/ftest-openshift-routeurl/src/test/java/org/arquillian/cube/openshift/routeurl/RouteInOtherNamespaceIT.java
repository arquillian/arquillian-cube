package org.arquillian.cube.openshift.routeurl;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift4;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift4
public class RouteInOtherNamespaceIT {

    @RouteURL(value = "prometheus-k8s", namespace = "openshift-monitoring")
    private URL routeURL;

    @Test
    public void injectedIntoClassShouldNotBeNull() {
        assertThat(routeURL).isNotNull();
    }

    @Test
    public void injectedIntoMethodShouldNotBeNull(
        @RouteURL(value = "prometheus-k8s", namespace = "openshift-monitoring") URL routeURL) {
        assertThat(routeURL).isNotNull();
    }

}
