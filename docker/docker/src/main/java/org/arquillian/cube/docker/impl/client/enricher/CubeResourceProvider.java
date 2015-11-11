package org.arquillian.cube.docker.impl.client.enricher;

import java.lang.annotation.Annotation;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import com.github.dockerjava.api.DockerClient;

public class CubeResourceProvider implements ResourceProvider {

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutor;

    @Override
    public boolean canProvide(Class<?> type) {
        return DockerClient.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        DockerClientExecutor dockerClientExec = this.dockerClientExecutor.get();

        if (dockerClientExec == null) {
            throw new IllegalStateException("Unable to inject DockerClient into test.");
        }

        return dockerClientExec.getDockerClient();
    }

}
