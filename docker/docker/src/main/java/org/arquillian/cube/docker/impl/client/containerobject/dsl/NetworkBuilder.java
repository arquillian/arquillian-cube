package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import org.arquillian.cube.docker.impl.client.config.IPAM;
import org.arquillian.cube.docker.impl.client.config.IPAMConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder to create a network object.
 */
public class NetworkBuilder {

    private static final String DEFAULT_NETWORK_DRIVER = "bridge";

    private String id;
    private String networkDriver;

    private NetworkBuilder(String id, String networkDriver) {
        this.networkDriver = networkDriver;
        this.id = id;
    }

    public static NetworkBuilder withDefaultDriver(String networkId) {
        return new NetworkBuilder(networkId, DEFAULT_NETWORK_DRIVER);
    }

    public static NetworkBuilder withDriver(String networkId, String driver) {
        return new NetworkBuilder(networkId, driver);
    }

    public IpamBuilder withIpam(String driver) {
        return new IpamBuilder(driver);
    }

    public Network build() {
        final org.arquillian.cube.docker.impl.client.config.Network network = new org.arquillian.cube.docker.impl.client.config.Network();
        network.setDriver(this.networkDriver);
        return new Network(this.id, network);

    }

    private Network build(IPAM ipam) {
        final org.arquillian.cube.docker.impl.client.config.Network network = new org.arquillian.cube.docker.impl.client.config.Network();
        network.setDriver(this.networkDriver);
        network.setIpam(ipam);
        return new Network(this.id, network);
    }

    public class IpamBuilder {

        private IPAM ipam = new IPAM();
        private List<IPAMConfig> configs = new ArrayList<>();

        public IpamBuilder(String driver) {
            ipam.setDriver(driver);
        }

        public IpamConfigurationBuilder withIpamConfiguration() {
            return new IpamConfigurationBuilder(this);
        }

        public Network build() {
            if (! configs.isEmpty()) {
                ipam.setIpamConfigs(configs);
            }

            return NetworkBuilder.this.build(ipam);
        }

    }

    public class IpamConfigurationBuilder {
        private IPAMConfig ipamConfig = new IPAMConfig();
        private IpamBuilder ipamBuilder;

        public IpamConfigurationBuilder(IpamBuilder ipamBuilder) {
            this.ipamBuilder = ipamBuilder;
        }

        public IpamConfigurationBuilder withSubnet(String subnet) {
            ipamConfig.setSubnet(subnet);
            return this;
        }

        public IpamConfigurationBuilder withGateway(String gateway) {
            ipamConfig.setGateway(gateway);
            return this;
        }

        public IpamConfigurationBuilder withIpRange(String ipRange) {
            ipamConfig.setIpRange(ipRange);
            return this;
        }

        public IpamBuilder add() {
            this.ipamBuilder.configs.add(this.ipamConfig);
            return this.ipamBuilder;
        }
    }

}
