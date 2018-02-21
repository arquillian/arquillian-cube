package org.arquillian.cube.openshift.impl.requirement;

import io.fabric8.kubernetes.clnt.v3_1.Config;
import io.fabric8.kubernetes.clnt.v3_1.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.utils.URLUtils;
import io.fabric8.openshift.clnt.v3_1.OpenShiftClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.impl.extension.ConfigurationRegistrar;

import static org.arquillian.cube.kubernetes.ClientConfigurator.getConfigBuilder;
import static org.arquillian.cube.kubernetes.impl.DefaultConfigurationFactory.KUBERNETES_EXTENSION_NAME;
import static org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfigurationFactory.OPENSHIFT_EXTENSION_NAME;

public class OpenshiftRequirement implements Constraint<RequiresOpenshift> {

    @Override
    public void check(RequiresOpenshift context) throws UnsatisfiedRequirementException {
        KubernetesClient client = new DefaultKubernetesClient(getConfig());

        OkHttpClient httpClient = client.adapt(OkHttpClient.class);
        Request versionRequest = new Request.Builder()
            .get()
            .url(URLUtils.join(client.getMasterUrl().toString(), "version"))
            .build();

        try {
            Response response = httpClient.newCall(versionRequest).execute();
            if (!response.isSuccessful()) {
                throw new UnsatisfiedRequirementException(
                    "Failed to verify Openshift version, due to: [" + response.message() + "]");
            } else if (!client.isAdaptable(OpenShiftClient.class)) {
                throw new UnsatisfiedRequirementException("A valid Kubernetes environmnet was found, but not Openshift.");
            }
        } catch (IOException e) {
            throw new UnsatisfiedRequirementException("Error while checking Openshift version: [" + e.getMessage() + "]");
        }
    }

    private Config getConfig() {
        final ConfigurationRegistrar configurationRegistrar = new ConfigurationRegistrar();
        final ArquillianDescriptor arquillian = configurationRegistrar.loadConfiguration();

        Map<String, String> map = new HashMap<>();
        map.putAll(arquillian.extension(KUBERNETES_EXTENSION_NAME).getExtensionProperties());
        map.putAll(arquillian.extension(OPENSHIFT_EXTENSION_NAME).getExtensionProperties());

        final CubeOpenShiftConfiguration config = CubeOpenShiftConfiguration.fromMap(map);

        return getConfigBuilder(config).build();
    }
}
