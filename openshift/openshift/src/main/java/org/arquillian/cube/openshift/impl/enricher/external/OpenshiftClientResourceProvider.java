package org.arquillian.cube.openshift.impl.enricher.external;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import io.fabric8.kubernetes.clnt.v2_2.Config;
import io.fabric8.kubernetes.clnt.v2_2.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v2_2.KubernetesClient;
import io.fabric8.openshift.clnt.v2_2.OpenShiftClient;

/**
 * A {@link ResourceProvider} for {@link OpenShiftClient}.
 */
public class OpenshiftClientResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(OpenShiftClient.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        KubernetesClient client = getClient();

        if (client == null) {
            throw new IllegalStateException("Unable to inject Kubernetes client into test.");
        } else if (!client.isAdaptable(OpenShiftClient.class)) {
            throw new IllegalStateException("Could not adapt to OpenShiftClient.");
        }

        return createUserClinet(client.adapt(OpenShiftClient.class));
    }

    private Object createUserClinet(OpenShiftClient client) {
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
