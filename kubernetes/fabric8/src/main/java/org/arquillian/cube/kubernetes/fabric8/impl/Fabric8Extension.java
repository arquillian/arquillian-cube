package org.arquillian.cube.kubernetes.fabric8.impl;

import io.fabric8.kubernetes.api.builder.v2_6.Visitor;
import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.LabelProvider;
import org.arquillian.cube.kubernetes.api.NamespaceService;
import org.arquillian.cube.kubernetes.fabric8.impl.annotation.Fabric8AnnotationProvider;
import org.arquillian.cube.kubernetes.fabric8.impl.label.Fabric8LabelProvider;
import org.arquillian.cube.kubernetes.fabric8.impl.visitor.SecretsAndServiceAccountVisitor;
import org.arquillian.cube.kubernetes.impl.annotation.DefaultAnnotationProvider;
import org.arquillian.cube.kubernetes.impl.label.DefaultLabelProvider;
import org.arquillian.cube.kubernetes.impl.namespace.DefaultNamespaceService;
import org.arquillian.cube.kubernetes.impl.visitor.ServiceAccountVisitor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class Fabric8Extension implements LoadableExtension {

    @Override
    public void register(LoadableExtension.ExtensionBuilder builder) {

        builder.service(NamespaceService.class, DefaultNamespaceService.class)
            .override(LabelProvider.class, DefaultLabelProvider.class, Fabric8LabelProvider.class)
            .override(AnnotationProvider.class, DefaultAnnotationProvider.class, Fabric8AnnotationProvider.class)
            .override(Visitor.class, ServiceAccountVisitor.class, SecretsAndServiceAccountVisitor.class);
    }
}
