package org.arquillian.cube.docker.junit.rule;

import org.arquillian.cube.docker.impl.client.containerobject.dsl.Network;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.NetworkBuilder;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.junit.DockerClientInitializer;
import org.arquillian.cube.spi.metadata.IsNetworkContainerObject;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkDslRule implements TestRule {

    private DockerClientExecutor dockerClientExecutor;

    private NetworkBuilder networkBuilder;
    private Network network;

    public NetworkDslRule(String networkId) {
        this.networkBuilder = Network.withDefaultDriver(networkId);
        initializeDockerClient();
    }

    public NetworkDslRule(String networkName, String driver) {
        this.networkBuilder = Network.withDriver(networkName, driver);
        initializeDockerClient();
    }

    private void initializeDockerClient() {
        this.dockerClientExecutor = DockerClientInitializer.initialize();
    }

    public List<String> getNetworks() {
        return  this.dockerClientExecutor.getNetworks().stream()
            .map(com.github.dockerjava.api.model.Network::getName)
            .collect(Collectors.toList());
    }

    public String getNetworkName() {
        return this.network.getId();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<>();
                network = networkBuilder.build();

                final org.arquillian.cube.docker.impl.client.config.Network dockerNetwork = network.getNetwork();
                String networkId = null;
                try {

                    networkId = dockerClientExecutor.createNetwork(network.getId(), dockerNetwork);
                    dockerNetwork.addMetadata(IsNetworkContainerObject.class, new IsNetworkContainerObject());

                    base.evaluate();

                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    if (networkId != null) {
                        dockerClientExecutor.removeNetwork(networkId);
                    }
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }
}
