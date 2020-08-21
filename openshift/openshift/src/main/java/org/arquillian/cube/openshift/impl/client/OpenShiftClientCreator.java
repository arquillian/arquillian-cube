package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.clnt.v4_10.Config;
import io.fabric8.kubernetes.clnt.v4_10.KubernetesClient;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

public class OpenShiftClientCreator {

    @Inject
    private Instance<KubernetesClient> kubernetesClientInstance;

    @Inject
    @ApplicationScoped
    private InstanceProducer<OpenShiftClient> openShiftClientProducer;

    public void createClient(@Observes AfterStart afterStart, Configuration conf) {

        if (!(conf instanceof CubeOpenShiftConfiguration)) {
            return;
        }

        final CubeOpenShiftConfiguration configuration = (CubeOpenShiftConfiguration) conf;

        final KubernetesClient kubernetesClient = kubernetesClientInstance.get();
        if (kubernetesClient != null && kubernetesClient.isAdaptable(io.fabric8.openshift.clnt.v4_10.OpenShiftClient.class)) {
            io.fabric8.openshift.clnt.v4_10.OpenShiftClient client = kubernetesClient.adapt(io.fabric8.openshift.clnt.v4_10.OpenShiftClient.class);
            openShiftClientProducer.set(
                createClient(client, client.getConfiguration(), client.getNamespace(), configuration.shouldKeepAliveGitServer()));

        }
    }

    public void clean(@Observes AfterSuite event, OpenShiftClient client) throws Exception {
        client.shutdown();
    }

    public OpenShiftClient createClient(io.fabric8.openshift.clnt.v4_10.OpenShiftClient client, Config openShiftConfig, String namespace, boolean keepAliveGitServer) {
        return new OpenShiftClient(client, openShiftConfig, namespace, keepAliveGitServer);
    }
}
