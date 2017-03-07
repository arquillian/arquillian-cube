package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.event.TestSuiteSection;
import org.arquillian.reporter.api.model.report.BasicReport;

public class DockerLogSection extends SectionEvent<DockerLogSection, BasicReport, TestSuiteSection> {

    public DockerLogSection() {
        super("DockerLog");
    }

    @Override
    public TestSuiteSection getParentSectionThisSectionBelongsTo() {
        return new TestSuiteSection();
    }

    @Override
    public Class<BasicReport> getReportTypeClass() {
        return BasicReport.class;
    }
}
