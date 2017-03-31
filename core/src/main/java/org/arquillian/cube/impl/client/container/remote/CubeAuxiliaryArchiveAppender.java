package org.arquillian.cube.impl.client.container.remote;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.impl.client.enricher.CubeControllerProvider;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.arquillian.cube.spi.requirement.Requires;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class CubeAuxiliaryArchiveAppender implements AuxiliaryArchiveAppender {

    @Override
    public Archive<?> createAuxiliaryArchive() {
        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "arquillian-cube.jar")
                .addPackage(CubeController.class.getPackage())
                .addPackages(true, CubeRemoteExtension.class.getPackage())
                .addClass(CubeControllerProvider.class)
                .addAsServiceProvider(RemoteLoadableExtension.class, CubeRemoteExtension.class);

        if (LoadableExtension.Validate.classExists("org.arquillian.cube.requirement.ArquillianConditionalRunner")) {
            javaArchive.addPackages(true, ArquillianConditionalRunner.class.getPackage(), Requires.class.getPackage());
        }

        return javaArchive;
    }
}
