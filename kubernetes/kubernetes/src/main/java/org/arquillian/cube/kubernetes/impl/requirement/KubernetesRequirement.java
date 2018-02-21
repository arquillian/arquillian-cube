package org.arquillian.cube.kubernetes.impl.requirement;

import io.fabric8.kubernetes.clnt.v3_1.Config;
import io.fabric8.kubernetes.clnt.v3_1.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.utils.URLUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;
import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.impl.extension.ConfigurationRegistrar;

import static org.arquillian.cube.kubernetes.ClientConfigurator.getConfigBuilder;
import static org.arquillian.cube.kubernetes.impl.DefaultConfigurationFactory.KUBERNETES_EXTENSION_NAME;

//TODO: The kubernetes client currently doesn't expose a method to do a version check. An issue has been raised, but until its done we do the work here. See https://github.com/fabric8io/kubernetes-client/issues/477.
public class KubernetesRequirement implements Constraint<RequiresKubernetes> {

    @Override
    public void check(RequiresKubernetes context) throws UnsatisfiedRequirementException {
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
                    "Failed to verify kubernetes version, due to: [" + response.message() + "]");
            }
        } catch (IOException e) {
            throw new UnsatisfiedRequirementException(
                "Error while checking kubernetes version: [" + e.getMessage() + "]");
        }
    }

    private Config getConfig() {
        final ConfigurationRegistrar configurationRegistrar = new ConfigurationRegistrar();
        final ArquillianDescriptor arquillian = configurationRegistrar.loadConfiguration();

        Map<String, String> map = new HashMap<>();
        map.putAll(arquillian.extension(KUBERNETES_EXTENSION_NAME).getExtensionProperties());

        final DefaultConfiguration config = DefaultConfiguration.fromMap(map);

        return getConfigBuilder(config).build();
    }
}
