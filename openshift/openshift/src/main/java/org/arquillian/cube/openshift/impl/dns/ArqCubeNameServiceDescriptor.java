package org.arquillian.cube.openshift.impl.dns;

/** rls TODO
import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;
**/
/**
 * ArqCubeNameServiceDescriptor
 * <p/>
 * Descriptor for OpenShift Route NameService.
 *
 * @author Rob Cernich
 */
public class ArqCubeNameServiceDescriptor
    /** rls TODO  https://github.com/arquillian/arquillian-cube/issues/1290
    implements NameServiceDescriptor
     **/
{
    /** rls TODO
    @Override

    public NameService createNameService() throws Exception {
        return new ArqCubeNameService();
    }
     **/
    /** rls TODO
    @Override
    **/
    public String getProviderName() {
        return "ArquillianCubeNameService";
    }

    /** rls TODO
    @Override
    **/
    public String getType() {
        return "dns";
    }

}
