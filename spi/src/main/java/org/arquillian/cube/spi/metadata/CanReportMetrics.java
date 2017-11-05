package org.arquillian.cube.spi.metadata;

import org.arquillian.reporter.api.builder.report.ReportInSectionBuilder;

/**
 * Metadata that adds metadata to a cube for reporting metrics to Arquillian Recorder
 */
public interface CanReportMetrics extends CubeMetadata {
    ReportInSectionBuilder report();
}
