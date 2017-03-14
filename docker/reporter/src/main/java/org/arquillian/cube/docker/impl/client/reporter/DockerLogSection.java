package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.event.TestSuiteSection;
import org.arquillian.reporter.api.model.report.BasicReport;

class DockerLogSection extends SectionEvent<DockerLogSection, BasicReport, TestSuiteSection> {
    private static final String DOCKER_LOG_ID = "DockerLog";

    private String testSuiteId;

    DockerLogSection() {
        super(DOCKER_LOG_ID);
    }

    DockerLogSection(String testSuiteId) {
        super(DOCKER_LOG_ID);
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
