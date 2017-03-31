package org.arquillian.cube.openshift.impl.locator;

import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.impl.locator.DefaultKubernetesResourceLocator;

public class OpenshiftKubernetesResourceLocator extends DefaultKubernetesResourceLocator {

    private static final String[] RESOURCE_NAMES =
        new String[] {"openshift", "META-INF/fabric8/openshift", "kubernetes", "META-INF/fabric8/kubernetes"};
    private static final String[] ALLOWED_SUFFIXES = {".json", ".yml", ".yaml"};

    @Override
    protected String[] getResourceNames() {
        return RESOURCE_NAMES;
    }

    @Override
    protected String[] getAllowedSuffixes() {
        return ALLOWED_SUFFIXES;
    }

    @Override
    public KubernetesResourceLocator toImmutable() {
        return this;
    }
}
