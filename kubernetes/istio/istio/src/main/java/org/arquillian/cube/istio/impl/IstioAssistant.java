package org.arquillian.cube.istio.impl;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.FileMatchProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.client.IstioClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.arquillian.cube.kubernetes.impl.utils.ResourceFilter;
import org.awaitility.Awaitility;

public class IstioAssistant {

    private final IstioClient istioClient;
    private final OkHttpClient httpClient;

    public IstioAssistant(IstioClient istioClient) {
        this.istioClient = istioClient;
        this.httpClient = new OkHttpClient();
    }

    public List<IstioResource> deployIstioResources(final InputStream inputStream) {
        return istioClient.registerCustomResources(inputStream);
    }

    public void undeployIstioResources(final List<IstioResource> istioResources) {
        for (IstioResource istioResource : istioResources) {
            istioClient.unregisterCustomResource(istioResource);
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
        return istioClient.registerCustomResources(content);
    }

    public void await(final URL url, Function<Response, Boolean> checker) {
        final Request request = new Request.Builder()
            .url(url)
            .build();

        this.await(request, checker);
    }

    public void await(final Request request, Function<Response, Boolean> checker) {
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .ignoreExceptions()
            .until(() -> {
                try (Response response = httpClient.newCall(request).execute()) {
                    return checker.apply(response);
                }
            });
    }
}
