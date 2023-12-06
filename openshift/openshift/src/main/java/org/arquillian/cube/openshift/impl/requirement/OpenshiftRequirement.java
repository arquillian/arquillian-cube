package org.arquillian.cube.openshift.impl.requirement;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.StandardHttpRequest;
import io.fabric8.kubernetes.client.okhttp.OkHttpClientFactory;
import io.fabric8.kubernetes.client.utils.URLUtils;
import io.fabric8.openshift.client.OpenShiftClient;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

        final Config httpClientConfig = new ClientConfigBuilder().configuration(config).build();
        try (KubernetesClient client = new DefaultKubernetesClient(httpClientConfig)) {

            HttpClient.Factory httpClientFactory = new OkHttpClientFactory();
            HttpClient httpClient = httpClientFactory.newBuilder(httpClientConfig).build();

            // TODO - check
            HttpRequest versionRequest =  new StandardHttpRequest.Builder()
                .url(new URL(URLUtils.join(client.getMasterUrl().toString(), "version").toString()))
                .method("GET", "*/*", null)
                .build();

            HttpResponse<String> response = httpClient.sendAsync(versionRequest, String.class).get();
            if (!response.isSuccessful()) {
                throw new UnsatisfiedRequirementException(
                    "Failed to verify Openshift version, due to: [" + response.message() + "]");
            } else if (!client.isAdaptable(OpenShiftClient.class)) {
                throw new UnsatisfiedRequirementException(
                    "A valid Kubernetes environmnet was found, but not Openshift.");
            }
        } catch (IOException | IllegalArgumentException | InterruptedException | ExecutionException e) {
            throw new UnsatisfiedRequirementException(
                "Error while checking Openshift version: [" + e.getMessage() + "]");
        }
    }
}
