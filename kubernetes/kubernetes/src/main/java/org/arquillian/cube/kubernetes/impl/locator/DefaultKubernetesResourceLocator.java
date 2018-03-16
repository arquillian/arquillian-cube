package org.arquillian.cube.kubernetes.impl.locator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import java.util.Optional;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;

public class DefaultKubernetesResourceLocator implements KubernetesResourceLocator {

    private static final String ROOT = "/";
    private static final String[] RESOURCE_NAMES = new String[] {"kubernetes", "META-INF/fabric8/kubernetes"};
    private static final String[] ALLOWED_SUFFIXES = {".json", ".yml", ".yaml"};

    @Override
    public URL locate() {
        for (String resource : getResourceNames()) {
            for (String suffix : getAllowedSuffixes()) {
                URL candidate = getResource(resource + suffix);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    public URL locateFromTargetDir() {
        for (String resource : getResourceNames()) {
            for (String suffix : getAllowedSuffixes()) {
                URL candidate = getResourceFromTarget(resource + suffix);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private URL getResourceFromTarget(String resourceName) {
        File targetDir = new File(System.getProperty("basedir", ".") + "/target/classes");

        try {
            return Files.walk(Paths.get(targetDir.toString()))
                .filter(path -> Files.isRegularFile(path) && path.endsWith(resourceName))
                .findFirst()
                .map(this::toUrl)
                .orElse(null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<URL> locateAdditionalResources() {
        return Collections.emptyList();
    }

    protected String[] getResourceNames() {
        return RESOURCE_NAMES;
    }

    protected String[] getAllowedSuffixes() {
        return ALLOWED_SUFFIXES;
    }

    private URL getResource(String resource) {
        return KubernetesResourceLocator.class.getResource(resource.startsWith(ROOT) ? resource : ROOT + resource);
    }

    @Override
    public KubernetesResourceLocator toImmutable() {
        return this;
    }
}
