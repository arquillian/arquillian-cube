package org.arquillian.cube.kubernetes.reporter;


import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.io.FilenameUtils;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.event.AfterStart;
import org.arquillian.cube.kubernetes.impl.event.Start;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.ReporterConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.FileEntry;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.KeyValueEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableCellEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableEntry;
import org.arquillian.recorder.reporter.model.entry.table.TableRowEntry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TakeKubernetesResourcesInformation {

    static final String REPLICAS = "Replicas";
    static final String STATUS = "Status";
    static final String CLUSTER_IP = "Cluster-IP";
    static final String PORTS = "Ports";
    static final String SERVICE = "Service";
    static final String REPLICATION_CONTROLLER = "Replication Controller";
    static final String POD = "Pod";
    static final String NAMESPACE = "Namespace";
    static final String MASTER_URL = "Master URL";
    static final String SESSION_STATUS = "Session Status";
    static final String CONFIGURATION = "Resources Configuration";

    @Inject
    Event<PropertyReportEvent> propertyReportEventEvent;

    @Inject
    Instance<DependencyResolver> dependencyResolver;

    public void reportKubernetesConfiguration(@Observes Start start, Configuration configuration, ReporterConfiguration reporterConfiguration) throws IOException {
        GroupEntry groupEntry = new GroupEntry(CONFIGURATION);
        Session session = start.getSession();

        if (configuration != null) {
            addFileEntries(groupEntry, getFilesForResourcesConfiguration(session, configuration, reporterConfiguration));
        }

        propertyReportEventEvent.fire(new PropertyReportEvent(groupEntry));
    }

    public void reportSessionStatus(@Observes AfterStart afterStart, KubernetesClient kubernetesClient) {

        Session session = afterStart.getSession();
        if (session != null) {
            String namespace = session.getNamespace();

            GroupEntry groupEntry = new GroupEntry(SESSION_STATUS);

            addEntry(groupEntry, getKeyValueEntry(NAMESPACE, namespace));
            addEntry(groupEntry, getKeyValueEntry(MASTER_URL, String.valueOf(kubernetesClient.getMasterUrl())));

            addEntry(groupEntry, getTableForReplicationControllers(kubernetesClient, namespace));
            addEntry(groupEntry, getTableForPods(kubernetesClient, namespace));
            addEntry(groupEntry, getTableForServices(kubernetesClient, namespace));

            propertyReportEventEvent.fire(new PropertyReportEvent(groupEntry));
        }
    }

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
    }

    private List<FileEntry> getFilesForResourcesConfiguration(Session session, Configuration configuration, ReporterConfiguration reporterConfiguration) throws IOException {
        final List<FileEntry> fileEntries = new ArrayList<>();
        URL environmentConfigUrl = configuration.getEnvironmentConfigUrl();

        if (environmentConfigUrl != null) {
            fileEntries.add(getFileForResourcesConfiguration(environmentConfigUrl, reporterConfiguration));
        }

        if (configuration.isEnvironmentInitEnabled()) {
            List<URL> dependencyUrls = !configuration.getEnvironmentDependencies().isEmpty() ? configuration.getEnvironmentDependencies() : dependencyResolver.get().resolve(session);

            for (URL dependencyUrl : dependencyUrls) {
                fileEntries.add(getFileForResourcesConfiguration(dependencyUrl, reporterConfiguration));
            }
        }

        return fileEntries;
    }

    private FileEntry getFileForResourcesConfiguration(URL url, ReporterConfiguration reporterConfiguration) throws IOException {


        final Path rootDir = Paths.get(reporterConfiguration.getRootDir().getAbsolutePath());
        final String filePath = relativizePath(url, rootDir);

        FileEntry fileEntry = new FileEntry();
        fileEntry.setPath(filePath);
        fileEntry.setMessage(CONFIGURATION);
        fileEntry.setType(getFileType(filePath));

        return fileEntry;
    }

    private String relativizePath(URL url, Path rootDir) {
        String filePath;
        final String pathURL = url.toString();

        if (pathURL.contains(rootDir.toString())) {
            final Path relativize = rootDir.relativize(Paths.get(url.getFile()));
            filePath = relativize.toString();
        } else {
            filePath = pathURL;
        }

        return filePath;
    }

    private KeyValueEntry getKeyValueEntry(String key, String value) {
        return new KeyValueEntry(key, value);
    }

    private String getPortsForService(Service service) {
        StringBuilder sb = new StringBuilder();

        for (ServicePort servicePort : service.getSpec().getPorts()) {
            sb.append(servicePort.getPort()).append(" ");
        }

        return sb.toString();
    }

    private void addEntry(GroupEntry groupEntry, PropertyEntry propertyEntry) {
        groupEntry.getPropertyEntries().add(propertyEntry);
    }

    private void addFileEntries(GroupEntry groupEntry, List<FileEntry> propertyEntries) {
        groupEntry.getPropertyEntries().addAll(propertyEntries);
    }

    private String getFileType(String path) throws IOException {
        String extension = FilenameUtils.getExtension(path);
        String type = "";
        if ("json".equals(extension)) {
            type = "application/json";
        } else if ("yml".equals(extension) || "yaml".equals(extension)) {
            type = "application/x-yaml";
        }
        return type;
    }
}
