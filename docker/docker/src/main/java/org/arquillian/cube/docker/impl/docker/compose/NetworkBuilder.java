package org.arquillian.cube.docker.impl.docker.compose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.docker.impl.client.config.IPAM;
import org.arquillian.cube.docker.impl.client.config.IPAMConfig;
import org.arquillian.cube.docker.impl.client.config.Network;

import static org.arquillian.cube.docker.impl.util.YamlUtil.asListOfMap;
import static org.arquillian.cube.docker.impl.util.YamlUtil.asMap;
import static org.arquillian.cube.docker.impl.util.YamlUtil.asMapOfStrings;
import static org.arquillian.cube.docker.impl.util.YamlUtil.asString;

public class NetworkBuilder {

    private static final String DRIVER = "driver";
    private static final String DRIVER_OPTS = "driver_opts";
    private static final String IPAM = "ipam";
    private static final String DEFAULT_NETWORK_DRIVER = "bridge";
    private static final String SUBNET = "subnet";
    private static final String GATEWAY = "gateway";
    private static final String IP_RANGE = "iprange";
    private static final String CONFIG = "config";

    private Network configuration;

    public NetworkBuilder() {
        this(new Network());
    }

    protected NetworkBuilder(Network configuration) {
        this.configuration = configuration;
    }

    public Network build(Map<String, Object> dockerComposeContainerDefinition) {
        if (dockerComposeContainerDefinition != null && dockerComposeContainerDefinition.containsKey(DRIVER)) {
            addDriver(asString(dockerComposeContainerDefinition, DRIVER));
        } else {
            addDriver(DEFAULT_NETWORK_DRIVER);
        }
        if (dockerComposeContainerDefinition != null) {
            if (dockerComposeContainerDefinition.containsKey(IPAM)) {
                addIpam(asMap(dockerComposeContainerDefinition, IPAM));
            }
            if (dockerComposeContainerDefinition.containsKey(DRIVER_OPTS)) {
                this.configuration.setOptions(asMapOfStrings(dockerComposeContainerDefinition, DRIVER_OPTS));
            }
        }

        return build();
    }

    public NetworkBuilder addDriver(String driver) {
        this.configuration.setDriver(driver);
        return this;
    }

    public NetworkBuilder withDefaultDriver() {
        this.configuration.setDriver(DEFAULT_NETWORK_DRIVER);
        return this;
    }

    public NetworkBuilder addIpam(Map<String, Object> ipamConfig) {
        IPAM ipam = new IPAM();
        if (ipamConfig != null) {
            if (ipamConfig.containsKey(DRIVER)) {
                ipam.setDriver(asString(ipamConfig, DRIVER));
            }
            if (ipamConfig.containsKey(CONFIG)) {
                ipam.setIpamConfigs(createIpamConfig(asListOfMap(ipamConfig, CONFIG)));
            }
        }
        this.configuration.setIpam(ipam);

        return this;
    }

    private List<IPAMConfig> createIpamConfig(Collection<Map<String, Object>> configs) {
        List<IPAMConfig> ipamConfigs = new ArrayList<>();
        for (Map<String, Object> ipam : configs) {
            if (ipam != null) {
                IPAMConfig ipamConfig = new IPAMConfig();
                if (ipam.containsKey(SUBNET)) {
                    ipamConfig.setSubnet(asString(ipam, SUBNET));
                }
                if (ipam.containsKey(GATEWAY)) {
                    ipamConfig.setGateway(asString(ipam, GATEWAY));
                }
                if (ipam.containsKey(IP_RANGE)) {
                    ipamConfig.setIpRange(asString(ipam, IP_RANGE));
                }
                ipamConfigs.add(ipamConfig);
            }
        }

        return ipamConfigs;
    }

    public Network build() {
        return this.configuration;
    }
}
