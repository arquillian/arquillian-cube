package org.arquillian.cube.kubernetes.impl.visitor;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class NamespaceVisitor extends TypedVisitor<ObjectMetaBuilder> {

    @Inject
    Instance<Configuration> configuration;


    @Override
    public void visit(ObjectMetaBuilder builder) {
        new ImmutableNamespaceVisitor(configuration.get()).visit(builder);
    }

    public static class ImmutableNamespaceVisitor extends TypedVisitor<ObjectMetaBuilder> {

        private final Configuration configuration;

        public ImmutableNamespaceVisitor(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void visit(ObjectMetaBuilder builder) {
                builder.withNamespace(configuration.getNamespace());
        }
    }
}
