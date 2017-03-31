package org.arquillian.cube.openshift.ftest;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class IntegrationSuite {

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
            .addAsWebResource(new StringAsset("weee"), "index.html")
            .addClass(EnvPrinter.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }
}
