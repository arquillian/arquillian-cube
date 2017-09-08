package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.builder.v2_6.TypedVisitor;
import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.kubernetes.clnt.v2_6.ConfigBuilder;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

public class OpenShiftClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<OpenShiftClient> openShiftClientProducer;

    public void createClient(@Observes AfterStart afterStart, Configuration conf) {
        if (!(conf instanceof CubeOpenShiftConfiguration)) {
            return;
        }
        CubeOpenShiftConfiguration configuration = (CubeOpenShiftConfiguration) conf;
        System.setProperty("KUBERNETES_TRUST_CERT", "true");
        // override defaults for master and namespace
        final Config config = new ConfigBuilder()
            .withMasterUrl(configuration.getMasterUrl().toString())
            .withNamespace(configuration.getNamespace())
            .withTrustCerts(true)
            .accept(new TypedVisitor<ConfigBuilder>() {
                @Override
                public void visit(ConfigBuilder b) {
                    b.withNoProxy(b.getNoProxy() == null ? new String[0] : b.getNoProxy());
                }
            }).build();

        openShiftClientProducer.set(
            createClient(config, configuration.getNamespace(), configuration.shouldKeepAliveGitServer()));
    }

    public void clean(@Observes AfterSuite event, OpenShiftClient client) throws Exception {
        client.shutdown();
    }

    public OpenShiftClient createClient(Config openShiftConfig, String namespace, boolean keepAliveGitServer) {
        return new OpenShiftClient(openShiftConfig, namespace, keepAliveGitServer);
    }
}
