package org.arquillian.cube.docker.impl.reporter;

import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.event.Standalone;
import org.arquillian.reporter.api.event.TestSuiteConfigurationSection;
import org.arquillian.reporter.api.model.report.BasicReport;

public class DockerContainerSection
    extends SectionEvent<DockerContainerSection, BasicReport, TestSuiteConfigurationSection> implements Standalone {

    private static final String CONFIGURATION_ID = "Docker Containers";
    private String testSuiteId;

    public DockerContainerSection(String configurationId) {
        super(configurationId);
    }

    public DockerContainerSection(String configurationId, String testSuiteId) {
        super(configurationId);
        this.testSuiteId = testSuiteId;
    }

    public static DockerContainerSection standalone() {
        return new DockerContainerSection(Standalone.getStandaloneId());
    }

    @Override
    public TestSuiteConfigurationSection getParentSectionThisSectionBelongsTo() {
        return new TestSuiteConfigurationSection(CONFIGURATION_ID, testSuiteId);
    }

    @Override
    public Class<BasicReport> getReportTypeClass() {
        return BasicReport.class;
    }
}
