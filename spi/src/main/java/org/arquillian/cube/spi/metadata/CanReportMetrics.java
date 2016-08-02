package org.arquillian.cube.spi.metadata;

import org.arquillian.recorder.reporter.Reportable;

/**
 * Metadata that adds metadata to a cube for reporting metrics to Arquillian Recorder
 */
public interface CanReportMetrics extends CubeMetadata {
    Reportable report();
}
