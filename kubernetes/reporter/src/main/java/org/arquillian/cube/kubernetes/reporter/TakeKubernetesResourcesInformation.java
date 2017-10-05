package org.arquillian.cube.kubernetes.reporter;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.arquillian.cube.kubernetes.impl.event.Start;
import org.arquillian.reporter.api.builder.Reporter;
import org.arquillian.reporter.api.builder.report.ReportBuilder;
import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.model.entry.FileEntry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.CONFIGURATION;
import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.KUBERNETES_SECTION_NAME;
import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.MASTER_URL;
import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.NAMESPACE;
import static org.arquillian.cube.kubernetes.reporter.KubernetesReportKey.SESSION_STATUS;

public class TakeKubernetesResourcesInformation {

    @Inject
    Event<SectionEvent> sectionEvent;

    @Inject
    Instance<DependencyResolver> dependencyResolver;

    @Inject
    Instance<KubernetesResourceLocator> resourceLocator;

    public void reportKubernetesConfiguration(@Observes Start start, Configuration configuration,
        org.arquillian.reporter.config.ReporterConfiguration reporterConfiguration) throws IOException {
        final ReportBuilder reportBuilder = Reporter.createReport(CONFIGURATION);
        Session session = start.getSession();
        if (configuration != null) {
            reportBuilder.addEntries(getFilesForResourcesConfiguration(session, configuration, reporterConfiguration));
        }

        Reporter.createReport(KUBERNETES_SECTION_NAME)
            .addReport(reportBuilder)
            .inSection(new KubernetesSection()).fire(sectionEvent);
    }

    public void reportSessionStatus(@Observes AfterStart afterStart, KubernetesClient kubernetesClient) {

        Session session = afterStart.getSession();
        if (session != null) {
            String namespace = session.getNamespace();

            Reporter.createReport(SESSION_STATUS)
                .addKeyValueEntry(NAMESPACE, namespace)
                .addKeyValueEntry(MASTER_URL, String.valueOf(kubernetesClient.getMasterUrl()))
                .inSection(new KubernetesSection())
                .asSubReport()
                .fire(sectionEvent);
        }
    }
/*
    private TableEntry getTableForReplicationControllers(KubernetesClient kubernetesClient, String namespace) {

        TableEntry tableEntry = createTableEntryWithTHead(REPLICATION_CONTROLLER, REPLICATION_CONTROLLER, REPLICAS);

        for (ReplicationController replicationController : kubernetesClient.replicationControllers().inNamespace(namespace).list().getItems()) {
            TableRowEntry tableRowEntry = new TableRowEntry();
            tableRowEntry.addCells(new TableCellEntry(replicationController.getMetadata().getName()), new TableCellEntry(String.valueOf(replicationController.getSpec().getReplicas())));
            tableEntry.getTableBody().getRows().add(tableRowEntry);

        }

        return tableEntry;
    }

    private TableEntry getTableForServices(KubernetesClient kubernetesClient, String namespace) {

        TableEntry tableEntry = createTableEntryWithTHead(SERVICE, SERVICE, CLUSTER_IP, PORTS);

        for (Service service : kubernetesClient.services().inNamespace(namespace).list().getItems()) {
            TableRowEntry tableRowEntry = new TableRowEntry();
            tableRowEntry.addCells(
                    new TableCellEntry(service.getMetadata().getName()),
                    new TableCellEntry(String.valueOf(service.getSpec().getClusterIP())),
                    new TableCellEntry(getPortsForService(service)));

            tableEntry.getTableBody().getRows().add(tableRowEntry);
        }

        return tableEntry;
    }

    private TableEntry getTableForPods(KubernetesClient kubernetesClient, String namespace) {

        TableEntry tableEntry = createTableEntryWithTHead(POD, POD, STATUS);

        for (Pod pod : kubernetesClient.pods().inNamespace(namespace).list().getItems()) {
            TableRowEntry tableRowEntry = new TableRowEntry();
            tableRowEntry.addCells(new TableCellEntry(pod.getMetadata().getName()), new TableCellEntry(pod.getStatus().getPhase()));
            tableEntry.getTableBody().getRows().add(tableRowEntry);

        }

        return tableEntry;
    }

    private TableEntry createTableEntryWithTHead(String name, String... heads) {
        TableEntry tableEntry = new TableEntry();
        tableEntry.setTableName(name);

        for (String head : heads) {
            tableEntry.getTableHead().getRow().addCell(new TableCellEntry(head));
        }

        return tableEntry;
    }*/

    private List<FileEntry> getFilesForResourcesConfiguration(Session session, Configuration configuration,
        org.arquillian.reporter.config.ReporterConfiguration reporterConfiguration) throws IOException {
        final List<FileEntry> fileEntries = new ArrayList<>();
        URL environmentConfigUrl = configuration.getEnvironmentConfigUrl();

        if (environmentConfigUrl == null) {
            KubernetesResourceLocator kubernetesResourceLocator = resourceLocator.get();
            if (kubernetesResourceLocator != null) {
                environmentConfigUrl = kubernetesResourceLocator.locate();
            }
        }

        if (environmentConfigUrl != null) {
            fileEntries.add(getFileForResourcesConfiguration(environmentConfigUrl, reporterConfiguration));
        }

        if (configuration.isEnvironmentInitEnabled()) {
            List<URL> dependencyUrls =
                !configuration.getEnvironmentDependencies().isEmpty() ? configuration.getEnvironmentDependencies()
                    : dependencyResolver.get().resolve(session);

            for (URL dependencyUrl : dependencyUrls) {
                fileEntries.add(getFileForResourcesConfiguration(dependencyUrl, reporterConfiguration));
            }
        }

        return fileEntries;
    }

    private FileEntry getFileForResourcesConfiguration(URL url,
        org.arquillian.reporter.config.ReporterConfiguration reporterConfiguration) throws IOException {

        final Path rootDir = Paths.get(reporterConfiguration.getRootDirectory());
        final String filePath = relativizePath(url, rootDir);

        return new org.arquillian.reporter.api.model.entry.FileEntry(filePath);
    }

    private String relativizePath(URL url, Path rootDir) {
        String filePath;
        final String pathURL = url.toString();
        final String absoluteRootDir = new File(rootDir.toString()).getAbsolutePath();

        if (pathURL.contains(absoluteRootDir)) {
            final Path rootDirPath = Paths.get(absoluteRootDir);
            final Path configFilePath = Paths.get(url.getFile());

            final Path relativize = rootDirPath.relativize(configFilePath);
            filePath = relativize.toString();
        } else {
            filePath = pathURL;
        }

        return filePath;
    }

    /*private String getPortsForService(Service service) {
        StringBuilder sb = new StringBuilder();

        for (ServicePort servicePort : service.getSpec().getPorts()) {
            sb.append(servicePort.getPort()).append(" ");
        }

        return sb.toString();
    }
*/
}
