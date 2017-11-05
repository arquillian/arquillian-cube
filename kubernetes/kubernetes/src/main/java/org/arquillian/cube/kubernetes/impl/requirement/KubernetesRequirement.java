package org.arquillian.cube.kubernetes.impl.requirement;

import io.fabric8.kubernetes.clnt.v2_6.DefaultKubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.utils.URLUtils;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

//TODO: The kubernetes client currently doesn't expose a method to do a version check. An issue has been raised, but until its done we do the work here. See https://github.com/fabric8io/kubernetes-client/issues/477.
public class KubernetesRequirement implements Requirement<RequiresKubernetes> {

    @Override
    public void check(RequiresKubernetes context) throws UnsatisfiedRequirementException {
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
                    "Failed to verify kubernetes version, due to: [" + response.message() + "]");
            }
        } catch (IOException e) {
            throw new UnsatisfiedRequirementException(
                "Error while checking kubernetes version: [" + e.getMessage() + "]");
        }
    }
}
