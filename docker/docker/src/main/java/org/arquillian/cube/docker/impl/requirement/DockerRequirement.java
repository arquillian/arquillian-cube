package org.arquillian.cube.docker.impl.requirement;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.base.Strings;
import org.arquillian.cube.docker.impl.client.CubeDockerConfigurationResolver;
import org.arquillian.cube.docker.impl.client.CubeDockerConfigurator;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

import java.util.HashMap;
import java.util.Map;

public class DockerRequirement implements Requirement<RequiresDocker> {

    private final CommandLineExecutor commandLineExecutor = new CommandLineExecutor();
    private final CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(commandLineExecutor),
            new Boot2Docker(commandLineExecutor),
            new OperatingSystemResolver().currentOperatingSystem().getFamily()
    );

    @Override
    public void check(RequiresDocker context) throws UnsatisfiedRequirementException {
        Map<String, String> config = resolver.resolve(new HashMap<String, String>());
        String serverUrl = config.get(CubeDockerConfigurator.DOCKER_HOST);
        if (Strings.isNullOrEmpty(serverUrl)) {
            throw new UnsatisfiedRequirementException("Could not resolve the docker server url.");
        } else if (!isDockerRunning(serverUrl)) {
            throw new UnsatisfiedRequirementException("No server is running on url:[" + serverUrl + "].");
        }
    }

    /**
     * @param serverUrl     The url to check if docker is running on.
     * @return              True if docker is running on the url.
     */
    private static boolean isDockerRunning(String serverUrl) {
        return getDockerVersion(serverUrl) != null;
    }


    /**
     * Returns the docker version.
     * @param serverUrl The serverUrl to use.
     * @return
     */
    private static Version getDockerVersion(String serverUrl) {
        try {
            DockerClient client = DockerClientBuilder.getInstance(serverUrl).build();
            return client.versionCmd().exec();
        } catch (Exception e) {
            return null;
        }
    }
}
