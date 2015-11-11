package org.arquillian.cube.impl.client.container.remote;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.impl.client.enricher.CubeControllerProvider;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class CubeAuxiliaryArchiveAppender implements AuxiliaryArchiveAppender {

    @Override
    public Archive<?> createAuxiliaryArchive() {
        return ShrinkWrap.create(JavaArchive.class, "arquillian-cube.jar")
                .addPackage(CubeController.class.getPackage())
                .addPackages(true, CubeRemoteExtension.class.getPackage())
                .addClass(CubeControllerProvider.class)
                .addAsServiceProvider(RemoteLoadableExtension.class, CubeRemoteExtension.class);
    }
}
