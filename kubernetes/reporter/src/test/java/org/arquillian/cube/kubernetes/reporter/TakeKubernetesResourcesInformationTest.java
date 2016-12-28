package org.arquillian.cube.kubernetes.reporter;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.server.mock.KubernetesMockServer;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;
import org.arquillian.cube.kubernetes.impl.DefaultSession;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.arquillian.cube.kubernetes.impl.log.SimpleLogger;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.FileEntry;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.KeyValueEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableCellEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableEntry;
import org.jboss.arquillian.core.api.Event;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.arquillian.cube.kubernetes.reporter.TakeKubernetesResourcesInformation.CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TakeKubernetesResourcesInformationTest {

    private static final KubernetesMockServer server = new KubernetesMockServer();

    private static final String TABLE_ENTRY = "TableEntry";
    private static final String KEY_VALUE_ENTRY = "KeyValueEntry";
    private static final String TABLE_HEAD = "tableHead";
    private static final String TABLE_BODY = "tableBody";
    private static final String CELLS = "cells";
    private static final String ROW = "row";
    private static final String ROWS = "rows";
    private static final String GET_CONTENT = "getContent";

    @Mock
    Event<PropertyReportEvent> propertyReportEvent;

    @Captor
    ArgumentCaptor<PropertyReportEvent> propertyReportEventArgumentCaptor;

    @BeforeClass
    public static void setUpClass() throws IOException {

        Pod testPod = new PodBuilder()
                .withNewMetadata()
                .withName("test-pod")
                .endMetadata()
                .withNewStatus()
                .withPhase("Running")
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

        ReplicationController testController = new ReplicationControllerBuilder()
                .withNewMetadata()
                .withName("test-controller")
                .endMetadata()
                .withNewSpec()
                .addToSelector("name", "somelabel")
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("name","somelabel")
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("test-container2")
                .withImage("test/image2")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();


        server.expect().get().withPath("/api/v1/namespaces/arquillian").andReturn(200, new NamespaceBuilder()
                .withNewMetadata()
                .withName("arquillian")
                .and().build()).always();


        //test-controller
        server.expect().get().withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller").andReturn(200, testController).always();
        server.expect().get().withPath("/api/v1/namespaces/arquillian/replicationcontrollers").andReturn(200, new ReplicationControllerListBuilder()
                .withItems(testController).build())
                .always();

        //test-pod
        server.expect().get().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(200, testPod).always();
        server.expect().get().withPath("/api/v1/namespaces/arquillian/pods").andReturn(200, new PodListBuilder()
                .withItems(testPod)
                .build()).always();

        //test-service
        server.expect().get().withPath("/api/v1/namespaces/arquillian/services/test-service").andReturn(200, testService).always();
        server.expect().get().withPath("/api/v1/namespaces/arquillian/services").andReturn(200, new ServiceListBuilder()
                .withItems(testService)
                .build()).always();
    }

    @Test
    public void should_report_configuration_file() throws IOException {
        //given
        Configuration configuration = getConfiguration();
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.propertyReportEventEvent = propertyReportEvent;

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(configuration, new ReporterConfiguration());

        //then
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();

        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);
        GroupEntry parent = (GroupEntry) propertyEntry;

        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(1);

        final PropertyEntry configFile = rootEntries.get(0);
        assertThat(configFile).isInstanceOf(FileEntry.class);
        assertThat(configFile).extracting("path", "message", "type").containsExactly(
                "test-classes/kubernetes.json", CONFIGURATION, "application/json");

    }

    @Test
    public void should_report_master_url_and_namespace() {

        String masterURL = "http://" + server.getHostName() + ":" + server.getPort() + "/";
        configureTakeKubernetesInformation();

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();

        List<PropertyEntry> rootEntries = assertPropertyEntryAndGetAllEntries(propertyReportEvent);

        assertSizeAndType(rootEntries);

        assertKeyValueForPropertEntry(rootEntries.get(0), TakeKubernetesResourcesInformation.NAMESPACE, "arquillian");
        assertKeyValueForPropertEntry(rootEntries.get(1), TakeKubernetesResourcesInformation.MASTER_URL, masterURL);

    }

    @Test
    public void should_report_replication_controllers() {
        configureTakeKubernetesInformation();

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();

        List<PropertyEntry> rootEntries = assertPropertyEntryAndGetAllEntries(propertyReportEvent);
        assertSizeAndType(rootEntries);

        PropertyEntry entryList = rootEntries.get(2);
        TableEntry tableEntry = (TableEntry) entryList;

        assertThat(tableEntry).extracting(TABLE_HEAD).extracting(ROW).flatExtracting(CELLS).
                hasSize(2).containsExactly(
                        new TableCellEntry(TakeKubernetesResourcesInformation.REPLICATION_CONTROLLER),
                        new TableCellEntry(TakeKubernetesResourcesInformation.REPLICAS));

        assertThat(tableEntry).extracting(TABLE_BODY).flatExtracting(ROWS).hasSize(1);
        assertThat(tableEntry.getTableBody().getRows().get(0).getCells()).hasSize(2).extractingResultOf(GET_CONTENT)
                .containsExactly("test-controller", "1");
    }

    @Test
    public void should_report_services() {
        configureTakeKubernetesInformation();

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();

        List<PropertyEntry> rootEntries = assertPropertyEntryAndGetAllEntries(propertyReportEvent);

        assertSizeAndType(rootEntries);

        PropertyEntry entryList = rootEntries.get(4);
        TableEntry tableEntry = (TableEntry) entryList;

        assertThat(tableEntry).extracting(TABLE_HEAD).extracting(ROW).flatExtracting(CELLS).
                hasSize(3).containsExactly(
                new TableCellEntry(TakeKubernetesResourcesInformation.SERVICE),
                new TableCellEntry(TakeKubernetesResourcesInformation.CLUSTER_IP),
                new TableCellEntry(TakeKubernetesResourcesInformation.PORTS));

        assertThat(tableEntry).extracting(TABLE_BODY).flatExtracting(ROWS).hasSize(1);
        assertThat(tableEntry.getTableBody().getRows().get(0).getCells()).hasSize(3).extractingResultOf(GET_CONTENT)
                .containsExactly("test-service", "10.0.0.1", "8080 ");
    }

    @Test
    public void should_report_pods() {
        configureTakeKubernetesInformation();

        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();

        List<PropertyEntry> rootEntries = assertPropertyEntryAndGetAllEntries(propertyReportEvent);
        assertSizeAndType(rootEntries);

        PropertyEntry entryList = rootEntries.get(3);
        TableEntry tableEntry = (TableEntry) entryList;

        assertThat(tableEntry).extracting(TABLE_HEAD).extracting(ROW).flatExtracting(CELLS).
                hasSize(2).containsExactly(
                new TableCellEntry(TakeKubernetesResourcesInformation.POD),
                new TableCellEntry(TakeKubernetesResourcesInformation.STATUS));

        assertThat(tableEntry).extracting(TABLE_BODY).flatExtracting(ROWS).hasSize(1);
        assertThat(tableEntry.getTableBody().getRows().get(0).getCells()).hasSize(2).extractingResultOf(GET_CONTENT)
                .containsExactly("test-pod", "Running");
    }

    private void assertKeyValueForPropertEntry(PropertyEntry propertyEntry, String key, String value) {
        KeyValueEntry keyValueEntry = (KeyValueEntry) propertyEntry;

        assertThat(keyValueEntry).extracting("key", "value").containsExactly(key, value);
    }

    private void assertSizeAndType(List<PropertyEntry> propertyEntries) {
        assertThat(propertyEntries).hasSize(5);
        assertThat(propertyEntries).extracting("class.simpleName").containsExactly(
                KEY_VALUE_ENTRY, KEY_VALUE_ENTRY, TABLE_ENTRY, TABLE_ENTRY, TABLE_ENTRY);
    }

    private TakeKubernetesResourcesInformation configureTakeKubernetesInformation() {
        Configuration configuration = getConfiguration();

        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.propertyReportEventEvent = propertyReportEvent;
        takeKubernetesResourcesInformation.reportSessionStatus(new AfterStart(new DefaultSession(configuration.getSessionId(), configuration.getNamespace(), new SimpleLogger())), server.createClient());

        return takeKubernetesResourcesInformation;
    }

    private Configuration getConfiguration() {
        Map<String, String> config = new LinkedHashMap();
        config.put(Configuration.NAMESPACE_TO_USE, "arquillian");
        config.put(Configuration.ENVIRONMENT_CONFIG_URL, TakeKubernetesResourcesInformationTest.class.getResource("/kubernetes.json").toString());

        return DefaultConfiguration.fromMap(config);

    }

    private List<PropertyEntry> assertPropertyEntryAndGetAllEntries (PropertyReportEvent propertyReportEvent) {
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();

        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);
        GroupEntry parent = (GroupEntry) propertyEntry;

        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();

        return rootEntries;
    }

}
