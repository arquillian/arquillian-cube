/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.builder.v2_6.Visitor;
import org.arquillian.cube.impl.client.enricher.StandaloneCubeUrlResourceProvider;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.ConfigurationFactory;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.FeedbackProvider;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.impl.annotation.AnnotationProviderRegistar;
import org.arquillian.cube.kubernetes.impl.annotation.DefaultAnnotationProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.ClientResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.DeploymentListResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.DeploymentResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.KuberntesServiceUrlResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.PodListResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.PodResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.ReplicaSetListResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.ReplicaSetResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.ReplicationControllerListResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.ReplicationControllerResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.ServiceListResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.external.ServiceResourceProvider;
import org.arquillian.cube.kubernetes.impl.enricher.SessionResourceProvider;
import org.arquillian.cube.kubernetes.impl.feedback.DefaultFeedbackProvider;
import org.arquillian.cube.kubernetes.impl.feedback.FeedbackProviderServiceRegistar;
import org.arquillian.cube.kubernetes.impl.install.DefaultResourceInstaller;
import org.arquillian.cube.kubernetes.impl.install.ResourceInstallerRegistar;
import org.arquillian.cube.kubernetes.impl.label.DefaultLabelProvider;
import org.arquillian.cube.kubernetes.impl.label.LabelProviderRegistar;
import org.arquillian.cube.kubernetes.impl.locator.DefaultKubernetesResourceLocator;
import org.arquillian.cube.kubernetes.impl.locator.KubernetesResourceLocatorRegistar;
import org.arquillian.cube.kubernetes.impl.log.LoggerRegistar;
import org.arquillian.cube.kubernetes.impl.namespace.DefaultNamespaceService;
import org.arquillian.cube.kubernetes.impl.namespace.NamespaceServiceRegistar;
import org.arquillian.cube.kubernetes.impl.resolve.DependencyResolverRegistar;
import org.arquillian.cube.kubernetes.impl.resolve.ShrinkwrapResolver;
import org.arquillian.cube.kubernetes.impl.visitor.DockerRegistryVisitor;
import org.arquillian.cube.kubernetes.impl.visitor.LoggingVisitor;
import org.arquillian.cube.kubernetes.impl.visitor.NamespaceVisitor;
import org.arquillian.cube.kubernetes.impl.visitor.ServiceAccountVisitor;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * An Arquillian extension for Kubernetes.
 */
public class KubernetesExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(ConfigurationRegistar.class)
            .observer(NamespaceServiceRegistar.class)
            .observer(KubernetesResourceLocatorRegistar.class)
            .observer(LabelProviderRegistar.class)
            .observer(DependencyResolverRegistar.class)
            .observer(AnnotationProviderRegistar.class)
            .observer(LoggerRegistar.class)
            .observer(ResourceInstallerRegistar.class)
            .observer(FeedbackProviderServiceRegistar.class)
            .observer(getClientCreator())
            .observer(SuiteListener.class)
            .observer(ClassListener.class)
            .observer(TestListener.class)
            .observer(SessionManagerLifecycle.class);

        builder.service(NamespaceService.class, DefaultNamespaceService.class)
            .service(KubernetesResourceLocator.class, DefaultKubernetesResourceLocator.class)
            .service(ResourceInstaller.class, DefaultResourceInstaller.class)
            .service(LabelProvider.class, DefaultLabelProvider.class)
            .service(DependencyResolver.class, ShrinkwrapResolver.class)
            .service(AnnotationProvider.class, DefaultAnnotationProvider.class)
            .service(FeedbackProvider.class, DefaultFeedbackProvider.class)
            .service(Visitor.class, LoggingVisitor.class)
            .service(Visitor.class, DockerRegistryVisitor.class)
            .service(Visitor.class, ServiceAccountVisitor.class)
            .service(Visitor.class, NamespaceVisitor.class)

            //External
            .service(ResourceProvider.class, ClientResourceProvider.class)
            .service(ResourceProvider.class, PodListResourceProvider.class)
            .service(ResourceProvider.class, PodResourceProvider.class)
            .service(ResourceProvider.class, DeploymentResourceProvider.class)
            .service(ResourceProvider.class, DeploymentListResourceProvider.class)
            .service(ResourceProvider.class, ReplicaSetResourceProvider.class)
            .service(ResourceProvider.class, ReplicaSetListResourceProvider.class)
            .service(ResourceProvider.class, ReplicationControllerListResourceProvider.class)
            .service(ResourceProvider.class, ReplicationControllerResourceProvider.class)
            .service(ResourceProvider.class, ServiceListResourceProvider.class)
            .service(ResourceProvider.class, ServiceResourceProvider.class)

            //Internal
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.ClientResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.PodListResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.PodResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.DeploymentResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.DeploymentListResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.ReplicaSetResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.ReplicaSetListResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.ReplicationControllerListResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.ReplicationControllerResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.ServiceListResourceProvider.class)
            .service(ResourceProvider.class, org.arquillian.cube.kubernetes.impl.enricher.internal.ServiceResourceProvider.class)

            .service(ResourceProvider.class, SessionResourceProvider.class)
            .service(ConfigurationFactory.class, DefaultConfigurationFactory.class)

            .override(ResourceProvider.class, StandaloneCubeUrlResourceProvider.class,
                KuberntesServiceUrlResourceProvider.class);
    }

    private Class getClientCreator() {
        Class creatorClass = null;
        String creatorClassName = System.getProperty(Constants.CLIENT_CREATOR_CLASS_NAME);
        try {
            if (Strings.isNotNullOrEmpty(creatorClassName)) {
                creatorClass = KubernetesExtension.class.getClassLoader().loadClass(creatorClassName);
            }
        } catch (Throwable t) {
            //fallback to default
        }
        return creatorClass != null ? creatorClass : ClientCreator.class;
    }
}
