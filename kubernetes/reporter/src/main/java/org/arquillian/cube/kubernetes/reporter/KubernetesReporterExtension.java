package org.arquillian.cube.kubernetes.reporter;

import org.arquillian.reporter.api.model.StringKey;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class KubernetesReporterExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        final boolean reportedInClasspath =
            Validate.classExists("org.arquillian.core.reporter.ArquillianCoreReporterExtension");
        if (reportedInClasspath) {
            builder.observer(TakeKubernetesResourcesInformation.class)
                .service(StringKey.class, KubernetesReportKey.class);
        }
    }
}
