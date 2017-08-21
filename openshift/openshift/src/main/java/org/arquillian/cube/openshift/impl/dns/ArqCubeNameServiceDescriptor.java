package org.arquillian.cube.openshift.impl.dns;


import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

/**
 * ArqCubeNameServiceDescriptor
 * <p/>
 * Descriptor for OpenShift Route NameService.
 *
 * @author Rob Cernich
 */
public class ArqCubeNameServiceDescriptor implements NameServiceDescriptor {

    @Override
    public NameService createNameService() throws Exception {
        return new ArqCubeNameService();
    }

    @Override
    public String getProviderName() {
        return "ArquillianCubeNameService";
    }

    @Override
    public String getType() {
        return "dns";
    }

}
