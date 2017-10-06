package org.arquillian.cube.openshift.impl.locator;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.openshift.clnt.v2_6.OpenShiftClient;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.impl.locator.DefaultKubernetesResourceLocator;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class OpenshiftKubernetesResourceLocator extends DefaultKubernetesResourceLocator {

    private static final String[] RESOURCE_NAMES =
        new String[] {"openshift", "META-INF/fabric8/openshift", "kubernetes", "META-INF/fabric8/kubernetes"};

    @Inject
    protected Instance<KubernetesClient> client;

    @Inject
    protected Instance<Configuration> configuration;

    @Override
    protected String[] getResourceNames() {
        if (!client.get().isAdaptable(OpenShiftClient.class)) {
            return super.getResourceNames();
        }
        return RESOURCE_NAMES;
    }

    @Override
    public Collection<URL> locateAdditionalResources() {
        if (!client.get().isAdaptable(OpenShiftClient.class)) {
            return super.locateAdditionalResources();
        }

        Configuration config = configuration.get();
        if (config instanceof CubeOpenShiftConfiguration && ((CubeOpenShiftConfiguration) config).isEnableImageStreamDetection()) {
            List<URL> additionalUrls = new LinkedList<>();
            File targetDir = new File(System.getProperty("basedir", ".") + "/target");
            if (targetDir.exists() && targetDir.isDirectory()) {
                File[] files = targetDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith("-is.yml")) {
                            try {
                                additionalUrls.add(file.toURI().toURL());
                            } catch (MalformedURLException e) {
                                // ignore
                            }
                        }
                    }
                }
            }
            return additionalUrls;
        }

        return Collections.emptyList();
    }
}
