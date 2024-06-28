package org.arquillian.cube.docker.impl.requirement;

import java.util.HashMap;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.base.Strings;

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.CubeDockerConfigurationResolver;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DefaultDocker;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.arquillian.spacelift.execution.ExecutionException;

public class DockerRequirement implements Constraint<RequiresDocker> {

    private final CommandLineExecutor commandLineExecutor;
    private final CubeDockerConfigurationResolver resolver;

    public DockerRequirement() {
        this.commandLineExecutor = new CommandLineExecutor();
        this.resolver = new CubeDockerConfigurationResolver(new Top(),
            new DefaultDocker(),
            new OperatingSystemResolver().currentOperatingSystem()
        );
    }

    public DockerRequirement(CommandLineExecutor commandLineExecutor, CubeDockerConfigurationResolver resolver) {
        this.commandLineExecutor = commandLineExecutor;
        this.resolver = resolver;
    }

    /**
     * @param serverUrl
     *     The url to check if docker is running on.
     *
     * @return True if docker is running on the url.
     */
    private static boolean isDockerRunning(String serverUrl) {
        return getDockerVersion(serverUrl) != null;
    }

    /**
     * Returns the docker version.
     *
     * @param serverUrl
     *     The serverUrl to use.
     */
    private static Version getDockerVersion(String serverUrl) {
        try {
            DockerClient client = DockerClientBuilder.getInstance(serverUrl).build();
            return client.versionCmd().exec();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void check(RequiresDocker context) throws UnsatisfiedRequirementException {
        try {
            Map<String, String> config = resolver.resolve(new HashMap<String, String>());
            String serverUrl = config.get(CubeDockerConfiguration.DOCKER_URI);
            if (Strings.isNullOrEmpty(serverUrl)) {
                throw new UnsatisfiedRequirementException("Could not resolve the docker server url.");
            } else if (!isDockerRunning(serverUrl)) {
                throw new UnsatisfiedRequirementException("No server is running on url:[" + serverUrl + "].");
            }
        } catch (ExecutionException e) {
            throw new UnsatisfiedRequirementException("Cannot execute docker command.");
        }
    }


}
