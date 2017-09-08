package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.Service;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.model.BuildablePodCube;
import org.arquillian.cube.openshift.impl.model.ServiceCube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeOpenShiftRegistrar {

    public void register(@Observes final OpenShiftClient client, final CubeRegistry registry, final Configuration conf,
        final Injector injector) {
        if (!(conf instanceof CubeOpenShiftConfiguration)) {
            return;
        }

        CubeOpenShiftConfiguration configuration = (CubeOpenShiftConfiguration) conf;
        if (!hasDefinitionStream(configuration)) {
            return;
        }

        for (HasMetadata item : client.getClientExt().load(getDefinitionStream(configuration)).get()) {
            if (item instanceof Pod) {
                registry.addCube(injector.inject(new BuildablePodCube((Pod) item, client, configuration)));
            } else if (item instanceof Service) {
                registry.addCube(injector.inject(new ServiceCube((Service) item, client, configuration)));
            }
        }
    }

    private boolean hasDefinitionStream(CubeOpenShiftConfiguration conf) {
        return conf.getDefinitions() != null || (conf.getDefinitionsFile() != null && new File(
            conf.getDefinitionsFile()).exists());
    }

    private InputStream getDefinitionStream(CubeOpenShiftConfiguration conf) {
        try {
            if (conf.getDefinitions() != null && !conf.getDefinitions().isEmpty()) {
                return new ByteArrayInputStream(conf.getDefinitions().getBytes(StandardCharsets.UTF_8));
            } else if (conf.getDefinitionsFile() != null && !conf.getDefinitionsFile().isEmpty()) {
                return new FileInputStream(conf.getDefinitionsFile());
            }
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("No definitions file found at " + conf.getDefinitionsFile());
        }
        //We've already check both
        throw new IllegalStateException("Neither definitions nor definitionsFile has been configured.");
    }
}
