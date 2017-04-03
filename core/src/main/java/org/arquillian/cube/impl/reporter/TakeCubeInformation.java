package org.arquillian.cube.impl.reporter;

import java.util.List;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterAutoStop;
import org.arquillian.cube.spi.metadata.CanReportMetrics;
import org.arquillian.reporter.api.builder.report.ReportInSectionBuilder;
import org.arquillian.reporter.api.event.SectionEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Observer that is executed before suite and prints all metrics reported by registered cubes.
 */
public class TakeCubeInformation {

    @Inject
    Event<SectionEvent> reportEvent;

    public void generateReportEntries(@Observes AfterAutoStop event, CubeRegistry cubeRegistry) {

        if (cubeRegistry == null) {
            return;
        }

        final List<Cube<?>> reportableCubes = cubeRegistry.getByMetadata(CanReportMetrics.class);

        for (Cube cube : reportableCubes) {
            final CanReportMetrics metadata = (CanReportMetrics) cube.getMetadata(CanReportMetrics.class);
            final ReportInSectionBuilder sectionBuilder = metadata.report();

            sectionBuilder.fire(reportEvent);
        }
    }
}
