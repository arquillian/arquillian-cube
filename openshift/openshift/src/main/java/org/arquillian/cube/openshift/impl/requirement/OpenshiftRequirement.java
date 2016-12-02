package org.arquillian.cube.openshift.impl.requirement;

import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

import java.io.IOException;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.URLUtils;
import io.fabric8.openshift.client.OpenShiftClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
                throw new UnsatisfiedRequirementException("Failed to verify Openshift version, due to: [" + response.message() + "]");
            } else if (!client.isAdaptable(OpenShiftClient.class)) {
                throw new UnsatisfiedRequirementException("A valid Kubernetes environmnet was found, but not Openshift.");
            }
        } catch (IOException e) {
            throw new UnsatisfiedRequirementException("Error while checking Openshift version: [" + e.getMessage() + "]");
        }
    }
}
