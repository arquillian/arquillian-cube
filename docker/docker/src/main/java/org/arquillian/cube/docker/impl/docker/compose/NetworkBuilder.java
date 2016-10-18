package org.arquillian.cube.docker.impl.docker.compose;

import static org.arquillian.cube.docker.impl.util.YamlUtil.asString;

import java.util.Map;

import org.arquillian.cube.docker.impl.client.config.Network;

public class NetworkBuilder {

    private static final String DRIVER = "driver";
    private static final String DRIVER_OPTS = "driver_opts";
    private static final String IPAM = "ipam";
    private static final String DEFAULT_NETWORK_DRIVER = "bridge";

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

    public Network build() {
        return this.configuration;
    }

}
