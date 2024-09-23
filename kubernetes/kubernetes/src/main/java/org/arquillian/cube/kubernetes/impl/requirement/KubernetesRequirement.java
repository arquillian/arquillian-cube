package org.arquillian.cube.kubernetes.impl.requirement;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.StandardHttpRequest;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;
import io.fabric8.kubernetes.client.utils.URLUtils;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.arquillian.cube.kubernetes.impl.ClientConfigBuilder;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;
import org.arquillian.cube.kubernetes.impl.ExtensionRegistrar;
import org.arquillian.cube.spi.requirement.Constraint;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;

import static org.arquillian.cube.kubernetes.impl.DefaultConfigurationFactory.KUBERNETES_EXTENSION_NAME;

//TODO: The kubernetes client currently doesn't expose a method to do a version check. An issue has been raised, but until its done we do the work here. See https://github.com/fabric8io/kubernetes-client/issues/477.
public class KubernetesRequirement implements Constraint<RequiresKubernetes> {
    HttpClient.Factory httpClientFactory = new JdkHttpClientFactory();

    @Override
    public void check(RequiresKubernetes context) throws UnsatisfiedRequirementException {

        final List<String> extension = Collections.singletonList(KUBERNETES_EXTENSION_NAME);

        final DefaultConfiguration config = new ExtensionRegistrar().loadExtension(extension);

        final Config httpClientConfig = new ClientConfigBuilder().configuration(config).build();
        try (KubernetesClient client = new DefaultKubernetesClient(httpClientConfig)) {

            HttpClient httpClient = httpClientFactory.newBuilder(httpClientConfig).build();
            HttpRequest versionRequest =  new StandardHttpRequest.Builder()
                .url(new URL(URLUtils.join(client.getMasterUrl().toString(), "version").toString()))
                .method("GET", "application/json", null)
                .build();

            HttpResponse<String> response = httpClient.sendAsync(versionRequest, String.class).get();
            if (!response.isSuccessful()) {
                throw new UnsatisfiedRequirementException(
                    "Failed to verify kubernetes version, due to: [" + response.message() + "]");
            }
        } catch (IOException | IllegalArgumentException | InterruptedException | ExecutionException e) {
            throw new UnsatisfiedRequirementException(
                "Error while checking kubernetes version: [" + e.getMessage() + "]");
        }
    }
}
