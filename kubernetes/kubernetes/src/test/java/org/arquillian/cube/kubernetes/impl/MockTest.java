package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.model.v2_6.Endpoints;
import io.fabric8.kubernetes.api.model.v2_6.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.v2_6.EndpointsListBuilder;
import io.fabric8.kubernetes.api.model.v2_6.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.PodBuilder;
import io.fabric8.kubernetes.api.model.v2_6.PodListBuilder;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationController;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationControllerListBuilder;
import io.fabric8.kubernetes.api.model.v2_6.Service;
import io.fabric8.kubernetes.api.model.v2_6.ServiceBuilder;
import io.fabric8.kubernetes.api.model.v2_6.ServiceListBuilder;
import io.fabric8.kubernetes.api.model.v2_6.WatchEvent;
import io.fabric8.kubernetes.api.model.v2_6.extensions.ReplicaSetBuilder;
import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.kubernetes.clnt.v2_6.server.mock.KubernetesMockServer;

import java.io.IOException;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(
    {
        PodInjection.class,
        ReplicationControllerInjection.class,
        ServiceInjection.class,
    }
)
@RequiresKubernetes
public class MockTest {

    private static final KubernetesMockServer MOCK = new KubernetesMockServer();

    @BeforeClass
    public static void setUpClass() throws IOException {

        Pod testPod = new PodBuilder()
            .withNewMetadata()
            .withName("test-pod")
            .endMetadata()
            .withNewStatus()
            .withPhase("Running")
            .addNewCondition()
            .withType("Ready")
            .withStatus("True")
            .endCondition()
            .endStatus()
            .build();

        Service testService = new ServiceBuilder()
            .withNewMetadata()
            .withName("test-service")
            .endMetadata()
            .withNewSpec()
            .withClusterIP("10.0.0.1")
            .addNewPort()
            .withPort(8080)
            .endPort()
            .endSpec()
            .build();

        Endpoints testEndpoints = new EndpointsBuilder()
            .withNewMetadata()
            .withName("test-service")
            .endMetadata()
            .build();

        Endpoints readyTestEndpoints = new EndpointsBuilder()
            .withNewMetadata()
            .withName("test-service")
            .withResourceVersion("2")
            .endMetadata()
            .addNewSubset()
            .addNewAddress()
            .withHostname("testhostname")
            .endAddress()
            .addNewPort()
            .withName("http")
            .withPort(8080)
            .endPort()
            .endSubset()
            .build();

        ReplicationController testController = new ReplicationControllerBuilder()
            .withNewMetadata()
            .withName("test-controller")
            .endMetadata()
            .withNewSpec()
            .addToSelector("name", "somelabel")
            .withReplicas(1)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("name", "somelabel")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("test-container2")
            .withImage("test/image2")
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .withNewStatus()
            .withReplicas(1)
            .withReadyReplicas(1)
            .endStatus()
            .build();

        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian").andReturn(200, new NamespaceBuilder()
            .withNewMetadata()
            .withName("arquillian")
            .and().build()).always();

        //test-controller
        MOCK.expect()
            .post()
            .withPath("/api/v1/namespaces/arquillian/replicationcontrollers")
            .andReturn(201, testController)
            .always();
        MOCK.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller")
            .andReturn(404, "")
            .once();
        MOCK.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller")
            .andReturn(200, testController)
            .always();
        MOCK.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/replicationcontrollers")
            .andReturn(200, new ReplicationControllerListBuilder()
                .withNewMetadata()
                .withResourceVersion("1")
                .endMetadata()
                .withItems(testController).build())
            .always();

        MOCK.expect()
            .delete()
            .withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller")
            .andReturn(200, "")
            .always();

        //test-pod
        MOCK.expect().post().withPath("/api/v1/namespaces/arquillian/pods").andReturn(201, testPod).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(404, "").once();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(200, testPod).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/pods").andReturn(200, new PodListBuilder()
            .withNewMetadata()
            .withResourceVersion("1")
            .endMetadata()
            .withItems(testPod)
            .build()).always();
        MOCK.expect().delete().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(200, "").always();

        //test-service
        MOCK.expect().post().withPath("/api/v1/namespaces/arquillian/services").andReturn(201, testService).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/services/test-service").andReturn(404, "").once();
        MOCK.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/services/test-service")
            .andReturn(200, testService)
            .always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/services").andReturn(200, new ServiceListBuilder()
            .withNewMetadata()
            .withResourceVersion("1")
            .endMetadata()
            .withItems(testService)
            .build()).always();
        MOCK.expect()
            .delete()
            .withPath("/api/v1/namespaces/arquillian/services/test-service")
            .andReturn(200, "")
            .always();

        //test-service endpoints
        MOCK.expect().post().withPath("/api/v1/namespaces/arquillian/endpoints").andReturn(201, testEndpoints).always();

        MOCK.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/endpoints/test-service")
            .andReturn(200, testEndpoints)
            .once();
        MOCK.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/endpoints/test-service")
            .andReturn(200, readyTestEndpoints)
            .always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/endpoints").andReturn(200, new EndpointsListBuilder()
            .withNewMetadata()
            .withResourceVersion("1")
            .endMetadata()
            .withItems(testEndpoints)
            .build()).always();

        MOCK.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/endpoints?resourceVersion=1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(1000)
            .andEmit(new WatchEvent(readyTestEndpoints, "MODIFIED"))
            .done()
            .always();

        MOCK.expect()
            .delete()
            .withPath("/api/v1/namespaces/arquillian/endpoints/test-service")
            .andReturn(200, "")
            .always();

        MOCK.expect()
            .get()
            .withPath("/apis/extensions/v1beta1/namespaces/arquillian/replicasets")
            .andReturn(200, new ReplicaSetBuilder().build())
            .always();

        MOCK.init();

        String masterUrl = MOCK.getServer().url("/").toString();
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, masterUrl);
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "arquillian");
        System.setProperty(Configuration.NAMESPACE_TO_USE, "arquillian");
        System.setProperty(Configuration.NAMESPACE_CLEANUP_ENABLED, "true");
        System.setProperty(Configuration.ENVIRONMENT_CONFIG_URL,
            MockTest.class.getResource("/test-kubernetes-1.json").toString());
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        //MOCK.destroy();
    }
}
