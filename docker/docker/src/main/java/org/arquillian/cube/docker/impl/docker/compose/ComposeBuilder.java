package org.arquillian.cube.docker.impl.docker.compose;

import static org.arquillian.cube.docker.impl.util.YamlUtil.asMap;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.Network;

public class ComposeBuilder {

	private DockerCompositions configuration;

	private static final String SERVICES = "services";
	private static final String NETWORKS = "networks";

	private static final String NETWORK_NAME_SUFFIX = "_default";

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
		if (dockerComposeContainerDefinition.containsKey(SERVICES)) {
			Map<String, Object> servicesDefinition = asMap(
					dockerComposeContainerDefinition, SERVICES);
			Set<String> keys = servicesDefinition.keySet();
			for (String key : keys) {
				ContainerBuilder services = new ContainerBuilder(this.dockerComposeRootLocation);
				CubeContainer cubeContainer = services.build(asMap(servicesDefinition, key));
				if (!dockerComposeContainerDefinition.containsKey(NETWORKS)) {
					String networkName = getDefaultNetworkName();
					cubeContainer.setNetworkMode(networkName);
				}
				this.configuration.add(key, cubeContainer);
			}
		}
		if (dockerComposeContainerDefinition.containsKey(NETWORKS)) {
			Map<String, Object> networksDefinition = asMap(
					dockerComposeContainerDefinition, NETWORKS);
			Set<String> keys = networksDefinition.keySet();
			for (String key : keys) {
				NetworkBuilder networks = new NetworkBuilder();
				Network network = networks.build(asMap(networksDefinition, key));
				this.configuration.add(key, network);
			}
		}  else {
			String networkName = getDefaultNetworkName();
			NetworkBuilder networkBuilder = new NetworkBuilder();
			Network network = networkBuilder.withDefaultDriver().build();
			this.configuration.add(networkName, network);
		}
		return this.configuration;
	}

	private String getDefaultNetworkName() {
		return this.dockerComposeRootLocation.toFile().getName() + NETWORK_NAME_SUFFIX;
	}

}
