package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.openshift.client.OpenShiftConfig;

import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

public class OpenShiftClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<OpenShiftClient> openShiftClientProducer;

    public void createClient(@Observes CubeOpenShiftConfiguration cubeConfiguration) {
        // System.setProperty(Configs.OPENSHIFT_CONFIG_FILE_PROPERTY,
        // "./src/test/resources/config.yaml");
        System.setProperty("KUBERNETES_TRUST_CERT", "true");
        final Config config = new Config();
        config.configFromSysPropsOrEnvVars(Config.builder().withMasterUrl(cubeConfiguration.getOriginServer())
                .withNamespace(cubeConfiguration.getNamespace()).withTrustCerts(true).build());
        if (config.getNoProxy() == null) {
            config.setNoProxy(new String[0]);
        }
        openShiftClientProducer.set(createClient(OpenShiftConfig.wrap(config), cubeConfiguration.getNamespace(),
                cubeConfiguration.shouldKeepAliveGitServer()));
    }

    public void clean(@Observes AfterSuite event, OpenShiftClient client) throws Exception {
        client.shutdown();
    }

    public OpenShiftClient createClient(OpenShiftConfig openShiftConfig, String namespace, boolean keepAliveGitServer) {
        return new OpenShiftClient(openShiftConfig, namespace, keepAliveGitServer);
    }
}
