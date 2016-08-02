package org.arquillian.cube.impl.reporter;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterAutoStop;
import org.arquillian.cube.spi.metadata.CanReportMetrics;
import org.arquillian.recorder.reporter.PropertyEntry;
import org.arquillian.recorder.reporter.Reportable;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.List;

/**
 * Observer that is executed before suite and prints all metrics reported by registered cubes.
 */
public class TakeCubeInformation {

    @Inject
    Event<PropertyReportEvent> propertyReportEvent;

    public void generatReportEntries(@Observes AfterAutoStop event, CubeRegistry cubeRegistry) {

        if (cubeRegistry == null) {
            return;
        }

        final List<Cube<?>> reportableCubes = cubeRegistry.getByMetadata(CanReportMetrics.class);

        for (Cube cube: reportableCubes) {
            final CanReportMetrics metadata = (CanReportMetrics) cube.getMetadata(CanReportMetrics.class);
            final Reportable report = metadata.report();

            if (report instanceof PropertyEntry) {
                propertyReportEvent.fire(new PropertyReportEvent((PropertyEntry) report));
            }
        }

    }

}
