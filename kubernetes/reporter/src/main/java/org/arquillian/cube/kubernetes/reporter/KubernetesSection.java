package org.arquillian.cube.kubernetes.reporter;

import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.event.TestSuiteSection;
import org.arquillian.reporter.api.model.report.BasicReport;

class KubernetesSection extends SectionEvent<KubernetesSection, BasicReport, TestSuiteSection> {

    private static final String KUBERNETES_ID = "k8s";
    private String testSuiteId;

    KubernetesSection() {
        super(KUBERNETES_ID);
    }

    KubernetesSection(String testSuiteId) {
        super(KUBERNETES_ID);
        this.testSuiteId = testSuiteId;
    }

    @Override
    public TestSuiteSection getParentSectionThisSectionBelongsTo() {
        return new TestSuiteSection(this.testSuiteId);
    }

    @Override
    public Class<BasicReport> getReportTypeClass() {
        return BasicReport.class;
    }
}
