package org.arquillian.cube.istio.impl;

import io.fabric8.istio.api.meta.v1alpha1.IstioStatus;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;

public class IstioResourceList extends DefaultKubernetesResourceList<IstioResource> {
}
