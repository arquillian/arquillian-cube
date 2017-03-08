package org.arquillian.cube.kubernetes.reporter;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TakeKubernetesResourcesInformationTest {

/*
    private static final KubernetesMockServer server = new KubernetesMockServer();

    private static final String TABLE_ENTRY = "TableEntry";
    private static final String KEY_VALUE_ENTRY = "KeyValueEntry";
    private static final String FILE_ENTRY = "FileEntry";
    private static final String TABLE_HEAD = "tableHead";
    private static final String TABLE_BODY = "tableBody";
    private static final String CELLS = "cells";
    private static final String ROW = "row";
    private static final String ROWS = "rows";
    private static final String GET_CONTENT = "getContent";
    private static final String CONTENT_TYPE = "application/json";
    private static final String TEST_CLASSES = "test-classes/";
    private static final String FILE_NAME = "kubernetes1.json";
    private static final String RELATIVE_PATH = TEST_CLASSES + Configuration.DEFAULT_CONFIG_FILE_NAME;
    private static final String SERVICES_FILE_NAME = "services.json";
    private static final String REPLICATION_CONTROLLER_FILE_NAME = "replication_controller.json";
    private static final String SERVICE_PATH = "http://foo.com/services.json";
    private static final String REPLICATION_CONTROLLER_PATH = "http://foo.com/replication_controller.json";

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
    public void should_report_environment_configuration_file_from_test_resources() throws IOException {
        //given
        Configuration configuration = DefaultConfiguration.fromMap(addEnvironmentConfigUrl(getConfig(), FILE_NAME));
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.reportEvent = propertyReportEvent;
        takeKubernetesResourcesInformation.dependencyResolver = getDependencyResolverInstance();

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)), configuration , new ReporterConfiguration());

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
                TEST_CLASSES + FILE_NAME, CONFIGURATION, CONTENT_TYPE);

    }

    @Test
    public void should_report_environment_configuration_file_from_default_location() throws IOException {
        //given
        Configuration configuration = getConfiguration();
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.reportEvent = propertyReportEvent;
        takeKubernetesResourcesInformation.dependencyResolver = getDependencyResolverInstance();

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)), configuration, new ReporterConfiguration());

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
                RELATIVE_PATH, CONFIGURATION, CONTENT_TYPE);

    }

    @Test
    public void should_report_environment_dependencies_from_http_url_and_configuration_from_default_location() throws IOException {
        //given
        String resourceName = SERVICE_PATH + " " + REPLICATION_CONTROLLER_PATH;
        Configuration configuration = DefaultConfiguration.fromMap(addEnvironmentDependencies(getConfig(), resourceName));
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.reportEvent = propertyReportEvent;
        takeKubernetesResourcesInformation.dependencyResolver = getDependencyResolverInstance();

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)), configuration, new ReporterConfiguration());

        //then
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();

        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);
        GroupEntry parent = (GroupEntry) propertyEntry;

        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(3);

        assertThat(rootEntries).extracting("class.simpleName").containsExactly(FILE_ENTRY, FILE_ENTRY, FILE_ENTRY);
        assertThat(rootEntries).flatExtracting("path", "message", "type").containsExactly(
                RELATIVE_PATH, CONFIGURATION, CONTENT_TYPE,
                SERVICE_PATH, CONFIGURATION, CONTENT_TYPE,
                REPLICATION_CONTROLLER_PATH, CONFIGURATION, CONTENT_TYPE);
    }

    @Test
    public void should_report_environment_dependencies_from_file_url_and_configuration_from_default_location() throws IOException {
        //given
        String resouceName = getResourceURL(SERVICES_FILE_NAME) + " " + getResourceURL(REPLICATION_CONTROLLER_FILE_NAME);
        System.out.println(resouceName);
        Configuration configuration = DefaultConfiguration.fromMap(addEnvironmentDependencies(getConfig(), resouceName));
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.reportEvent = propertyReportEvent;
        takeKubernetesResourcesInformation.dependencyResolver = (() -> new ShrinkwrapResolver("pom.xml", false));

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)), configuration, new ReporterConfiguration());

        //then
        verify(propertyReportEvent).fire(propertyReportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = propertyReportEventArgumentCaptor.getValue();
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();

        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);
        GroupEntry parent = (GroupEntry) propertyEntry;

        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();
        assertThat(rootEntries).hasSize(3);

        assertThat(rootEntries).extracting("class.simpleName").containsExactly(FILE_ENTRY, FILE_ENTRY, FILE_ENTRY);
        assertThat(rootEntries).flatExtracting("path", "message", "type").containsExactly(
                RELATIVE_PATH, CONFIGURATION, CONTENT_TYPE,
                TEST_CLASSES + SERVICES_FILE_NAME, CONFIGURATION, CONTENT_TYPE,
                TEST_CLASSES + REPLICATION_CONTROLLER_FILE_NAME, CONFIGURATION, CONTENT_TYPE);
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
                new TableCellEntry(REPLICATION_CONTROLLER),
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
        takeKubernetesResourcesInformation.reportEvent = propertyReportEvent;
        takeKubernetesResourcesInformation.reportSessionStatus(new AfterStart(getDefaultSession(configuration)), server.createClient());

        return takeKubernetesResourcesInformation;
    }

    private Session getDefaultSession(Configuration configuration) {
        return new DefaultSession(configuration.getSessionId(), configuration.getNamespace(), new SimpleLogger());
    }

    private Configuration getConfiguration() {
        return DefaultConfiguration.fromMap(getConfig());
    }

    private Map<String, String> addEnvironmentConfigUrl(Map<String, String> config, String resourceName) {
        config.put(Configuration.ENVIRONMENT_CONFIG_URL, getResourceURL(resourceName));

        return config;
    }

    private String getResourceURL(String resourceName) {
        return TakeKubernetesResourcesInformationTest.class.getResource("/" + resourceName).toString();
    }

    private Map<String, String> getConfig() {
        Map<String, String> config = new LinkedHashMap();
        config.put(Configuration.NAMESPACE_TO_USE, "arquillian");

        return config;
    }

    private Map<String, String> addEnvironmentDependencies(Map<String, String> config, String resourceName) {

        config.put(Configuration.ENVIRONMENT_DEPENDENCIES, resourceName);

        return config;
    }

    private List<PropertyEntry> assertPropertyEntryAndGetAllEntries(PropertyReportEvent propertyReportEvent) {
        final PropertyEntry propertyEntry = propertyReportEvent.getPropertyEntry();

        assertThat(propertyEntry).isInstanceOf(GroupEntry.class);
        GroupEntry parent = (GroupEntry) propertyEntry;

        final List<PropertyEntry> rootEntries = parent.getPropertyEntries();

        return rootEntries;
    }

    private Instance<DependencyResolver> getDependencyResolverInstance() {
        return () -> new ShrinkwrapResolver("pom.xml", false);
    }
*/

}
