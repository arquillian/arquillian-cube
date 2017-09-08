package org.arquillian.cube.openshift.impl.requirement;

import io.fabric8.kubernetes.clnt.v2_6.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.utils.URLUtils;
import io.fabric8.openshift.clnt.v2_6.OpenShiftClient;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

public class OpenshiftRequirement implements Requirement<RequiresOpenshift> {

    @Override
    public void check(RequiresOpenshift context) throws UnsatisfiedRequirementException {
        KubernetesClient client = new DefaultKubernetesClient();

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
