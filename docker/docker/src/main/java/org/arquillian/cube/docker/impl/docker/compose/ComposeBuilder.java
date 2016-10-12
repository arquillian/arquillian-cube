package org.arquillian.cube.docker.impl.docker.compose;

import static org.arquillian.cube.docker.impl.util.YamlUtil.asMap;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Network;

public class ComposeBuilder {

	private DockerCompositions configuration;

	private static final String SERVICES = "services";
	private static final String NETWORKS = "networks";

	private static final String NETWORK_NAME_SUFFIX = "default";

	private Path dockerComposeRootLocation;

	ComposeBuilder(Path dockerComposeRootLocation) {
		this(dockerComposeRootLocation, new DockerCompositions());
	}

	private ComposeBuilder(Path dockerComposeRootLocation,
	                       DockerCompositions configuration) {
		this.dockerComposeRootLocation = dockerComposeRootLocation;
		this.configuration = configuration;
	}

	@SuppressWarnings("unchecked")
	public DockerCompositions build(Map<String, Object> dockerComposeContainerDefinition) {
        Map<String, String> networkNames = new LinkedHashMap<>();
        if (dockerComposeContainerDefinition.containsKey(NETWORKS)) {
            Map<String, Object> networksDefinition = asMap(
                    dockerComposeContainerDefinition, NETWORKS);
            Set<String> keys = networksDefinition.keySet();

            for (String key : keys) {
                NetworkBuilder networks = new NetworkBuilder();
                String networkName = getComposeRootLocation() + key ;
                Network network = networks.build(asMap(networksDefinition, key));
                this.configuration.add(networkName, network);
                networkNames.put(key, networkName);
            }
        } else {
            String networkName = getDefaultNetworkName();
            NetworkBuilder networkBuilder = new NetworkBuilder();
            Network network = networkBuilder.withDefaultDriver().build();
            this.configuration.add(networkName, network);
        }
		if (dockerComposeContainerDefinition.containsKey(SERVICES)) {
			Map<String, Object> servicesDefinition = asMap(
					dockerComposeContainerDefinition, SERVICES);
			Set<String> keys = servicesDefinition.keySet();
			for (String key : keys) {
				ContainerBuilder services = new ContainerBuilder(this.dockerComposeRootLocation);
				CubeContainer cubeContainer = services.build(asMap(servicesDefinition, key), DockerComposeConverter.DOCKER_COMPOSE_VERSION_2_VALUE);
				Map<String, Object> serviceDefinition = asMap(servicesDefinition, key);
                if (!dockerComposeContainerDefinition.containsKey(NETWORKS)) {
					String networkName = getDefaultNetworkName();
					cubeContainer.setNetworkMode(networkName);
                    Collection<Network> nwList = new HashSet<>();
                    nwList.add(this.configuration.getNetworks().get(networkName));
                    cubeContainer.setNetworks(nwList);
				} else {
					if (serviceDefinition.containsKey(NETWORKS)) {
                        ArrayList<String> networks = (ArrayList) serviceDefinition.get(NETWORKS);
                        if (networks.size() >= 1) {
                            String networkName = networkNames.get(networks.get(0));
                            cubeContainer.setNetworkMode(networkName);
                            Collection<Network> nwList = new HashSet<>();
                            nwList.add(this.configuration.getNetworks().get(networkName));
                            cubeContainer.setNetworks(nwList);
                        } else {
                            throw new IllegalArgumentException("networks not mentioned under services networks section.");
                        }
                    } else {
                        Map<String, Network> nws = this.configuration.getNetworks();
                        String networkName = getDefaultNetworkName();
                        Collection<Network> nwList = new HashSet<>();
                        if (!nws.containsKey(networkName)) {
                            NetworkBuilder networkBuilder = new NetworkBuilder();
                            Network network = networkBuilder.withDefaultDriver().build();
                            this.configuration.add(networkName, network);
                        }
                        cubeContainer.setNetworkMode(networkName);
                        nwList.add(this.configuration.getNetworks().get(networkName));
                        cubeContainer.setNetworks(nwList);
                    }
				}
                String serviceName = getComposeRootLocation() + key;
                this.configuration.add(serviceName, cubeContainer);
			}
		}
		return this.configuration;
	}

	private String getDefaultNetworkName() {
		return  getComposeRootLocation() + NETWORK_NAME_SUFFIX;
	}

	private String getComposeRootLocation() {
        return this.dockerComposeRootLocation.toFile().getAbsoluteFile().getName() + "_";
    }
}
