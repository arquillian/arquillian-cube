package org.arquillian.cube.istio.impl;

import io.fabric8.istio.client.IstioClient;
import io.fabric8.istio.client.V1alpha3APIGroupDSL;
import io.fabric8.istio.client.V1beta1APIGroupDSL;
import io.fabric8.kubernetes.api.model.APIGroup;
import io.fabric8.kubernetes.api.model.APIGroupList;
import io.fabric8.kubernetes.api.model.APIResourceList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.RequestConfig;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.http.HttpClient;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class IstioClientAdapter implements IstioClient {

    private final IstioClient istioClient;
    private final MixedOperation<IstioResource, IstioResourceList, Resource<IstioResource>> resourceOperation;


    public IstioClientAdapter(final IstioClient istioClient) {
        this.istioClient = istioClient;
        this.resourceOperation = istioClient.resources(IstioResource.class, IstioResourceList.class);
    }

    public List<IstioResource> registerCustomResources(final InputStream resource) {
        return Collections.singletonList(resourceOperation.load(resource).create());
    }

    public List<IstioResource> registerCustomResources(final String resource) {
        return Collections.singletonList(resourceOperation.load(resource).create());
    }

    public Boolean unregisterCustomResource(final IstioResource istioResource) {
        return resourceOperation.resource(istioResource).delete().stream().allMatch(d -> d.getCauses().isEmpty());
    }

    public IstioClient unwrap() {
        return istioClient;
    }

    @Override
    public V1beta1APIGroupDSL v1beta1() {
        return unwrap().v1beta1();
    }

    @Override
    public V1alpha3APIGroupDSL v1alpha3() {
        return unwrap().v1alpha3();
    }

    @Override
    public <C extends Client> Boolean isAdaptable(Class<C> type) {
        return unwrap().isAdaptable(type);
    }

    @Override
    public <R extends KubernetesResource> boolean supports(Class<R> type) {
        return unwrap().supports(type);
    }

    @Override
    public boolean supports(String apiVersion, String kind) {
        return unwrap().supports(apiVersion, kind);
    }

    @Override
    public boolean hasApiGroup(String apiGroup, boolean exact) {
        return unwrap().hasApiGroup(apiGroup, exact);
    }

    @Override
    public <C extends Client> C adapt(Class<C> type) {
        return unwrap().adapt(type);
    }

    @Override
    public URL getMasterUrl() {
        return unwrap().getMasterUrl();
    }

    @Override
    public String getApiVersion() {
        return unwrap().getApiVersion();
    }

    @Override
    public String getNamespace() {
        return unwrap().getNamespace();
    }

    @Override
    public RootPaths rootPaths() {
        return unwrap().rootPaths();
    }

    @Override
    public boolean supportsApiPath(String path) {
        return unwrap().supportsApiPath(path);
    }

    @Override
    public void close() {
        unwrap().close();
    }

    @Override
    public APIGroupList getApiGroups() {
        return unwrap().getApiGroups();
    }

    @Override
    public APIGroup getApiGroup(String name) {
        return unwrap().getApiGroup(name);
    }

    @Override
    public APIResourceList getApiResources(String groupVersion) {
        return unwrap().getApiResources(groupVersion);
    }

    @Override
    public <T extends HasMetadata, L extends KubernetesResourceList<T>, R extends Resource<T>> MixedOperation<T, L, R> resources(Class<T> resourceType, Class<L> listClass, Class<R> resourceClass) {
        return unwrap().resources(resourceType, listClass, resourceClass);
    }

    @Override
    public Client newClient(RequestConfig requestConfig) {
        return unwrap().newClient(requestConfig);
    }

    @Override
    public HttpClient getHttpClient() {
        return unwrap().getHttpClient();
    }

    @Override
    public Config getConfiguration() {
        return unwrap().getConfiguration();
    }

    @Override
    public String raw(String uri, String method, Object payload) {
        return unwrap().raw(uri, method, payload);
    }
}
