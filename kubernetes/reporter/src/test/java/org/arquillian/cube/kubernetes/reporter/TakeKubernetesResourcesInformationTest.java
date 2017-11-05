package org.arquillian.cube.kubernetes.reporter;

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
import io.fabric8.kubernetes.clnt.v2_6.server.mock.KubernetesMockServer;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;
import org.arquillian.cube.kubernetes.impl.DefaultSession;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.arquillian.cube.kubernetes.impl.event.Start;
import org.arquillian.cube.kubernetes.impl.log.SimpleLogger;
import org.arquillian.cube.kubernetes.impl.resolve.ShrinkwrapResolver;
import org.arquillian.reporter.api.builder.BuilderLoader;
import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.model.StringKey;
import org.arquillian.reporter.api.model.entry.FileEntry;
import org.arquillian.reporter.api.model.entry.KeyValueEntry;
import org.arquillian.reporter.api.model.report.BasicReport;
import org.arquillian.reporter.api.model.report.Report;
import org.arquillian.reporter.config.ReporterConfiguration;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.CONFIGURATION;
import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.KUBERNETES_SECTION_NAME;
import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.MASTER_URL;
import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.NAMESPACE;
import static org.arquillian.reporter.impl.asserts.ReportAssert.assertThatReport;
import static org.arquillian.reporter.impl.asserts.SectionAssert.assertThatSection;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TakeKubernetesResourcesInformationTest {

    private static final KubernetesMockServer server = new KubernetesMockServer();
    /*
        private static final String TABLE_ENTRY = "TableEntry";
        private static final String TABLE_HEAD = "tableHead";
        private static final String TABLE_BODY = "tableBody";
        private static final String CELLS = "cells";
        private static final String ROW = "row";
        private static final String ROWS = "rows";*/
    private static final String TEST_CLASSES = "test-classes/";
    private static final String FILE_NAME = "kubernetes1.json";
    private static final String RELATIVE_PATH = TEST_CLASSES + Configuration.DEFAULT_CONFIG_FILE_NAME;
    private static final String SERVICES_FILE_NAME = "services.json";
    private static final String REPLICATION_CONTROLLER_FILE_NAME = "replication_controller.json";
    private static final String SERVICE_PATH = "http://foo.com/services.json";
    private static final String REPLICATION_CONTROLLER_PATH = "http://foo.com/replication_controller.json";

    @Mock
    Event<SectionEvent> sectionEvent;

    @Captor
    ArgumentCaptor<SectionEvent> reportEventArgumentCaptor;

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
        server.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller")
            .andReturn(200, testController)
            .always();
        server.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/replicationcontrollers")
            .andReturn(200, new ReplicationControllerListBuilder()
                .withItems(testController).build())
            .always();

        //test-pod
        server.expect().get().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(200, testPod).always();
        server.expect().get().withPath("/api/v1/namespaces/arquillian/pods").andReturn(200, new PodListBuilder()
            .withItems(testPod)
            .build()).always();

        //test-service
        server.expect()
            .get()
            .withPath("/api/v1/namespaces/arquillian/services/test-service")
            .andReturn(200, testService)
            .always();
        server.expect().get().withPath("/api/v1/namespaces/arquillian/services").andReturn(200, new ServiceListBuilder()
            .withItems(testService)
            .build()).always();
    }

    @Before
    public void setUp() {
        BuilderLoader.load();
    }

    @Test
    public void should_report_environment_configuration_file_from_test_resources() throws IOException {
        //given
        Configuration configuration = DefaultConfiguration.fromMap(addEnvironmentConfigUrl(getConfig(), FILE_NAME));
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.sectionEvent = sectionEvent;
        takeKubernetesResourcesInformation.dependencyResolver = getDependencyResolverInstance();
        takeKubernetesResourcesInformation.resourceLocator = getKubernetesResourceLocator();

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)),
            configuration, getReporterConfiguration());

        //then
        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());
        final SectionEvent sectionEvent = reportEventArgumentCaptor.getValue();
        final Report report = sectionEvent.getReport();

        assertSectionEvent(sectionEvent, KUBERNETES_SECTION_NAME);

        final List<Report> subReports = report.getSubReports();
        assertThatReport(subReports.get(0))
            .hasName(CONFIGURATION)
            .hasNumberOfEntries(1)
            .hasEntriesContaining(new FileEntry(TEST_CLASSES + FILE_NAME));
    }

    @Test
    public void should_report_environment_configuration_file_from_default_location() throws IOException {
        //given
        Configuration configuration = getConfiguration();
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.sectionEvent = sectionEvent;
        takeKubernetesResourcesInformation.dependencyResolver = getDependencyResolverInstance();
        takeKubernetesResourcesInformation.resourceLocator = getKubernetesResourceLocator();

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)),
            configuration, getReporterConfiguration());

        //then
        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());
        final SectionEvent sectionEvent = reportEventArgumentCaptor.getValue();
        final Report report = sectionEvent.getReport();

        assertSectionEvent(sectionEvent, KUBERNETES_SECTION_NAME);

        final List<Report> subReports = report.getSubReports();
        assertThatReport(subReports.get(0))
            .hasName(CONFIGURATION)
            .hasNumberOfEntries(1)
            .hasEntriesContaining(new FileEntry(RELATIVE_PATH));
    }

    @Test
    public void should_report_environment_dependencies_from_http_url_and_configuration_from_default_location()
        throws IOException {
        //given
        String resourceName = SERVICE_PATH + " " + REPLICATION_CONTROLLER_PATH;
        Configuration configuration = DefaultConfiguration.fromMap(addEnvironmentDependencies(getConfig(), resourceName));
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.sectionEvent = sectionEvent;
        takeKubernetesResourcesInformation.dependencyResolver = getDependencyResolverInstance();
        takeKubernetesResourcesInformation.resourceLocator = getKubernetesResourceLocator();

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)),
            configuration, getReporterConfiguration());

        //then
        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());
        final SectionEvent sectionEvent = reportEventArgumentCaptor.getValue();
        final Report report = sectionEvent.getReport();

        assertSectionEvent(sectionEvent, KUBERNETES_SECTION_NAME);

        final List<Report> subReports = report.getSubReports();
        assertThatReport(subReports.get(0))
            .hasName(CONFIGURATION)
            .hasNumberOfEntries(3)
            .hasEntriesContaining(
                new FileEntry(RELATIVE_PATH),
                new FileEntry(SERVICE_PATH),
                new FileEntry(REPLICATION_CONTROLLER_PATH));
    }

    @Test
    public void should_report_environment_dependencies_from_file_url_and_configuration_from_default_location()
        throws IOException {
        //given
        String resouceName = getResourceURL(SERVICES_FILE_NAME) + " " + getResourceURL(REPLICATION_CONTROLLER_FILE_NAME);
        Configuration configuration = DefaultConfiguration.fromMap(addEnvironmentDependencies(getConfig(), resouceName));
        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.sectionEvent = sectionEvent;
        takeKubernetesResourcesInformation.dependencyResolver = getDependencyResolverInstance();
        takeKubernetesResourcesInformation.resourceLocator = getKubernetesResourceLocator();

        //when
        takeKubernetesResourcesInformation.reportKubernetesConfiguration(new Start(getDefaultSession(configuration)),
            configuration, getReporterConfiguration());

        //then
        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());

        final SectionEvent sectionEvent = reportEventArgumentCaptor.getValue();
        final Report report = sectionEvent.getReport();

        assertSectionEvent(sectionEvent, KUBERNETES_SECTION_NAME);

        final List<Report> subReports = report.getSubReports();
        assertThatReport(subReports.get(0))
            .hasName(CONFIGURATION)
            .hasNumberOfEntries(3)
            .hasEntriesContaining(
                new FileEntry(RELATIVE_PATH),
                new FileEntry(TEST_CLASSES + SERVICES_FILE_NAME),
                new FileEntry(TEST_CLASSES + REPLICATION_CONTROLLER_FILE_NAME));
    }

    @Test
    public void should_report_master_url_and_namespace() {

        String masterURL = "http://" + server.getHostName() + ":" + server.getPort() + "/";
        configureTakeKubernetesInformation();

        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());

        final SectionEvent sectionEvent = reportEventArgumentCaptor.getValue();
        final Report report = sectionEvent.getReport();

        assertThatSection(sectionEvent).hasSectionId("k8s").hasReportOfTypeThatIsAssignableFrom(BasicReport.class);

        assertThatReport(sectionEvent.getReport())
            .hasNumberOfEntries(2);

        assertThatReport(report).hasEntriesContaining(
            new KeyValueEntry(NAMESPACE, "arquillian"),
            new KeyValueEntry(MASTER_URL, masterURL));
    }

    /*@Test
    public void should_report_replication_controllers() {
        configureTakeKubernetesInformation();

        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = reportEventArgumentCaptor.getValue();

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

        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = reportEventArgumentCaptor.getValue();

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

        verify(sectionEvent).fire(reportEventArgumentCaptor.capture());

        final PropertyReportEvent propertyReportEvent = reportEventArgumentCaptor.getValue();

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

*/
    private void assertSectionEvent(SectionEvent sectionEvent, StringKey name) {
        assertThatSection(sectionEvent).hasSectionId("k8s").hasReportOfTypeThatIsAssignableFrom(BasicReport.class);

        assertThatReport(sectionEvent.getReport())
            .hasName(name)
            .hasNumberOfEntries(0).hasNumberOfSubReports(1);
    }

    private TakeKubernetesResourcesInformation configureTakeKubernetesInformation() {
        Configuration configuration = getConfiguration();

        TakeKubernetesResourcesInformation takeKubernetesResourcesInformation = new TakeKubernetesResourcesInformation();
        takeKubernetesResourcesInformation.sectionEvent = sectionEvent;
        takeKubernetesResourcesInformation.reportSessionStatus(new AfterStart(getDefaultSession(configuration)),
            server.createClient());

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

    private Instance<DependencyResolver> getDependencyResolverInstance() {
        return () -> new ShrinkwrapResolver("pom.xml", false);
    }

    private Instance<KubernetesResourceLocator> getKubernetesResourceLocator() {
        return () -> new KubernetesResourceLocator() {
            @Override
            public URL locate() {
                return getClass().getResource("/kubernetes.json");
            }

            @Override
            public Collection<URL> locateAdditionalResources() {
                return Collections.emptyList();
            }

            @Override
            public KubernetesResourceLocator toImmutable() {
                return this;
            }
        };
    }

    public ReporterConfiguration getReporterConfiguration() {
        return ReporterConfiguration.fromMap(new LinkedHashMap<>());
    }
}
