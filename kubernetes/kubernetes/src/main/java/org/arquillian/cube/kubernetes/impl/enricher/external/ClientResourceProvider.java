package org.arquillian.cube.kubernetes.impl.enricher.external;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.kubernetes.clnt.v2_6.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.clnt.v2_6.KubernetesClient}.
 */
public class ClientResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(KubernetesClient.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        KubernetesClient client = getClient();

        if (client == null) {
            throw new IllegalStateException("Unable to inject Kubernetes client into test.");
        }
        return createUserClient(client);
    }


    private Object createUserClient(KubernetesClient client) {
        Config config = client.getConfiguration();

        Object userConfig = toUsersResource(config);
        Class userConfigClass = loadClass(internalToUserType(config.getClass().getName()));
        Class userClientClass = loadClass(internalToUserType(DefaultKubernetesClient.class.getName()));
        try {
            Constructor<?> constructor = userClientClass.getConstructor(userConfigClass);
            return constructor.newInstance(userConfig);
        } catch (Throwable t) {
           return null;
        }
    }
}
