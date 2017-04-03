package org.arquillian.cube.spi.metadata;

import java.net.InetAddress;
import java.util.Set;

/**
 * Port bindings for the container. This includes basic container details (IP,
 * exposed ports) as well as mappings for specific container ports.
 *
 * @author Rob Cernich
 */
public interface HasPortBindings extends CubeMetadata {

    /**
     * Returns true if the container has been bound. Prior to being bound, the
     * container's IP address may not be valid and port information will be
     * specific to what is configured for the container (e.g. EXPOSEd ports).
     * After the container is bound, the ports list may change (e.g. if the
     * container was started with -p &lt;port&gt;:&lt;not-exposed-port&gt;).
     *
     * @return true if the container has been bound.
     */
    boolean isBound();

    /**
     * @return the container's IP address, may be null if the container has not
     * been bound to an IP.
     */
    String getContainerIP();

    /**
     * @return the container's internal IP address, may be null if the container has not
     * been bound to an IP. The internal ip is as seen by the Host or other containers.
     */
    String getInternalIP();

    /**
     * @return list of configured container ports.
     */
    Set<Integer> getContainerPorts();

    /**
     * @return list of all container ports (configured and dynamically bound
     * (e.g. -p &lt;port&gt;:&lt;not-exposed-port&gt;)
     */
    Set<Integer> getBoundPorts();

    /**
     * @param targetPort
     *     the target port
     *
     * @return the mapped address for the target port, may be null if the port
     * is bound, but not mapped (e.g. no -p :port)
     */
    PortAddress getMappedAddress(int targetPort);

    /**
     * @return the mapped address in the arquillian.xml,defined by the
     * portForwardBindAddress property.
     * If null, returns the default - 127.0.0.1.
     */
    InetAddress getPortForwardBindAddress();

    /**
     * Port mapping for a bound port. The socket address that should be used to
     * configure network connections to the specified contianer port.
     */
    public interface PortAddress {
        /**
         * @return the host IP
         */
        String getIP();

        /**
         * @return the host port
         */
        int getPort();
    }

    /**
     * Simple implementation of PortAddress
     */
    public static final class PortAddressImpl implements PortAddress {
        private final String ip;
        private final int port;

        public PortAddressImpl(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public String getIP() {
            return ip;
        }

        @Override
        public int getPort() {
            return port;
        }
    }
}
