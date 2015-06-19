package org.arquillian.cube.openshift.impl.client;

import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

import io.fabric8.kubernetes.api.KubernetesFactory;

public class OpenShiftClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<OpenShiftClient> openShiftClientProducer;

    public void createClient(@Observes CubeOpenShiftConfiguration cubeConfiguration) {
        openShiftClientProducer.set(
                createClient(
                        cubeConfiguration.getOriginServer(),
                        cubeConfiguration.getNamespace(),
                        cubeConfiguration.shouldKeepAliveGitServer()));
    }

    public void clean(@Observes AfterSuite event, OpenShiftClient client) throws Exception {
        client.shutdown();
    }

    public OpenShiftClient createClient(String originServer, String namespace, boolean keepAliveGitServer) {
        //System.setProperty(Configs.OPENSHIFT_CONFIG_FILE_PROPERTY, "./src/test/resources/config.yaml");
        System.setProperty("KUBERNETES_TRUST_CERT", "true");

        KubernetesFactory factory = new KubernetesFactory(originServer);

        return new OpenShiftClient(factory, namespace, keepAliveGitServer);
    }
}
