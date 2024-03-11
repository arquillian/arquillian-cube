package org.arquillian.cube.istio.impl;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.FileMatchProcessor;
import org.arquillian.cube.kubernetes.impl.utils.ResourceFilter;
import org.awaitility.Awaitility;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class IstioAssistant {

    private final HttpClient.Factory httpClientFactory = new JdkHttpClientFactory();
    private final HttpClient httpClient;
    private final IstioClientAdapter istioClientAdapter;

    public IstioAssistant(IstioClientAdapter istioClientAdapter) {
        this.httpClient = httpClientFactory.newBuilder().build();
        this.istioClientAdapter = istioClientAdapter;
    }

    public List<IstioResource> deployIstioResources(final InputStream inputStream) {
        return istioClientAdapter.registerCustomResources(inputStream);
    }

    public void undeployIstioResource(final IstioResource istioResource) {
        istioClientAdapter.unregisterCustomResource(istioResource);
    }

    public void undeployIstioResources(final List<IstioResource> istioResources) {
        for (IstioResource istioResource : istioResources) {
            undeployIstioResource(istioResource);
        }
    }

    public List<IstioResource> deployIstioResources(final URL...urls) throws IOException {

        final List<IstioResource> istioResources = new ArrayList<>();

        for (URL url : urls) {
            try (final InputStream inputStream = url.openStream()) {
                istioResources.addAll(this.deployIstioResources(inputStream));
            }
        }

        return istioResources;
    }

    /**
     * Deploys application reading resources from classpath, matching the given regular expression.
     * For example istio/.*\\.json will deploy all resources ending with json placed at istio classpath directory.
     *
     * @param pattern to match the resources.
     */
    public List<IstioResource> deployIstioResourcesFromClasspathPattern(String pattern) {

        final List<IstioResource> istioResources = new ArrayList<>();
        final FastClasspathScanner fastClasspathScanner = new FastClasspathScanner();

        fastClasspathScanner.matchFilenamePattern(pattern, (FileMatchProcessor) (relativePath, inputStream, lengthBytes) -> {
            istioResources.addAll(deployIstioResources(inputStream));
            inputStream.close();
        }).scan();

        return istioResources;
    }

    /**
     * Deploys all y(a)ml and json files located at given directory.
     *
     * @param directory       where resources files are stored
     * @throws IOException
     */
    public List<IstioResource> deployIstioResources(final Path directory) throws IOException {

        final List<IstioResource> istioResources = new ArrayList<>();

        if (Files.isDirectory(directory)) {
            Files.list(directory)
                .filter(ResourceFilter::filterKubernetesResource)
                .map(p -> {
                    try {
                        return Files.newInputStream(p);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .forEach(is -> {
                    try {
                        istioResources.addAll(deployIstioResources(is));
                        is.close();
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                });
        } else {
            throw new IllegalArgumentException(String.format("%s should be a directory", directory));
        }

        return istioResources;
    }

    /**
     * Deploy Istio definition provided as string
     * @param content
     * @return
     */
    public List<IstioResource> deployIstioResources(String content) {
        return istioClientAdapter.registerCustomResources(content);
    }

    public void await(final URL url, Function<HttpResponse<String>, Boolean> checker) throws URISyntaxException {
        final HttpRequest request = httpClient.newHttpRequestBuilder()
            .uri(url.toURI().toString())
            .build();

        this.await(request, checker);
    }

    public void await(final HttpRequest request, Function<HttpResponse<String>, Boolean> checker) {
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .ignoreExceptions()
            .until(() -> {
                return checker.apply(httpClient.sendAsync(request, String.class).get());
            });
    }
}
