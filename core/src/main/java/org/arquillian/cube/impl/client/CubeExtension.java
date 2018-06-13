package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.client.container.ContainerConfigurationController;
import org.arquillian.cube.impl.client.container.ContainerMetadataUpdater;
import org.arquillian.cube.impl.client.container.CubeContainerLifecycleController;
import org.arquillian.cube.impl.client.container.CubeRemoteCommandObserver;
import org.arquillian.cube.impl.client.container.ProtocolMetadataUpdater;
import org.arquillian.cube.impl.client.container.remote.CubeAuxiliaryArchiveAppender;
import org.arquillian.cube.impl.client.enricher.CubeControllerProvider;
import org.arquillian.cube.impl.client.enricher.CubeIDResourceProvider;
import org.arquillian.cube.impl.client.enricher.CubeIpTestEnricher;
import org.arquillian.cube.impl.client.enricher.HostIpTestEnricher;
import org.arquillian.cube.impl.client.enricher.HostPortTestEnricher;
import org.arquillian.cube.impl.client.enricher.StandaloneCubeUrlResourceProvider;
import org.arquillian.cube.impl.reporter.DockerReportKey;
import org.arquillian.cube.impl.reporter.TakeCubeInformation;
import org.arquillian.reporter.api.model.StringKey;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class CubeExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(CubeConfigurator.class)
            .observer(CubeRegistrar.class)
            .observer(CubeLifecycleController.class)
            //.observer(CubeSuiteLifecycleController.class)
            .observer(ClientCubeControllerCreator.class)
            .observer(ForceStopDockerContainersShutdownHook.class);

        builder.service(ResourceProvider.class, CubeControllerProvider.class)
            .service(TestEnricher.class, HostIpTestEnricher.class)
            .service(TestEnricher.class, CubeIpTestEnricher.class)
            .service(TestEnricher.class, HostPortTestEnricher.class);

        // Arquillian Container integration
        // Only register if container-test-spi is on classpath
        if (Validate.classExists("org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender")) {
            builder.observer(ContainerMetadataUpdater.class)
                .observer(ProtocolMetadataUpdater.class)
                .observer(CubeContainerLifecycleController.class)
                .observer(ContainerConfigurationController.class)
                .observer(CubeRemoteCommandObserver.class);
            builder.service(AuxiliaryArchiveAppender.class, CubeAuxiliaryArchiveAppender.class);
        } else {
            // It is standalone
            builder.service(ResourceProvider.class, StandaloneCubeUrlResourceProvider.class);
        }

        // Only register if container-test-impl is on classpath
        if (Validate.classExists(
            "org.jboss.arquillian.container.test.impl.enricher.resource.OperatesOnDeploymentAwareProvider")) {
            builder.service(ResourceProvider.class, CubeIDResourceProvider.class);
        }

        // Only if reporter is in classpath we should provide reporting capabilities.
        if (Validate.classExists("org.arquillian.core.reporter.ArquillianCoreReporterExtension")) {
            builder.observer(TakeCubeInformation.class)
                .service(StringKey.class, DockerReportKey.class);
        }
    }
}
