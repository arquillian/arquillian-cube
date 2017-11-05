package org.arquillian.cube.kubernetes.reporter;

import org.arquillian.reporter.api.model.AbstractStringKey;
import org.arquillian.reporter.api.model.StringKey;

public class KubernetesReportKey extends AbstractStringKey {
    static final StringKey REPLICAS = new KubernetesReportKey();
    static final StringKey STATUS = new KubernetesReportKey();
    static final StringKey CLUSTER_IP = new KubernetesReportKey();
    static final StringKey PORTS = new KubernetesReportKey();
    static final StringKey SERVICE = new KubernetesReportKey();
    static final StringKey REPLICATION_CONTROLLER = new KubernetesReportKey();
    static final StringKey POD = new KubernetesReportKey();
    static final StringKey NAMESPACE = new KubernetesReportKey();
    static final StringKey MASTER_URL = new KubernetesReportKey();
    static final StringKey SESSION_STATUS = new KubernetesReportKey();
    static final StringKey CONFIGURATION = new KubernetesReportKey();
    static final StringKey KUBERNETES_SECTION_NAME = new KubernetesReportKey();
}
