package org.arquillian.cube.openshift.impl.requirement;

import io.fabric8.kubernetes.clnt.v4_0.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v4_0.KubernetesClient;
import io.fabric8.kubernetes.clnt.v4_0.utils.URLUtils;
import io.fabric8.openshift.clnt.v4_0.OpenShiftClient;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.kubernetes.impl.ClientConfigBuilder;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;
import org.arquillian.cube.kubernetes.impl.ExtensionRegistrar;
import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

import static org.arquillian.cube.kubernetes.impl.DefaultConfigurationFactory.KUBERNETES_EXTENSION_NAME;
import static org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfigurationFactory.OPENSHIFT_EXTENSION_NAME;

public class OpenshiftRequirement implements Constraint<RequiresOpenshift> {

    @Override
    public void check(RequiresOpenshift context) throws UnsatisfiedRequirementException {
        final List<String> extension = Arrays.asList(KUBERNETES_EXTENSION_NAME, OPENSHIFT_EXTENSION_NAME);

        final DefaultConfiguration config = new ExtensionRegistrar().loadExtension(extension);

        KubernetesClient client = new DefaultKubernetesClient(new ClientConfigBuilder().configuration(config).build());

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
}
