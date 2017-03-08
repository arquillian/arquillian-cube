package org.arquillian.cube.kubernetes.reporter;


import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.event.TestSuiteSection;
import org.arquillian.reporter.api.model.report.BasicReport;

public class KubernetesSection extends SectionEvent<KubernetesSection, BasicReport, TestSuiteSection>{

    public KubernetesSection() {
        super("k8s");
    }

    @Override
    public TestSuiteSection getParentSectionThisSectionBelongsTo() {
        return new TestSuiteSection() ;
    }

    @Override
    public Class<BasicReport> getReportTypeClass() {
        return BasicReport.class;
    }
}
