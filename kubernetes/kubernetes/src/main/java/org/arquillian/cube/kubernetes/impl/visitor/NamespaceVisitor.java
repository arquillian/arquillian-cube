package org.arquillian.cube.kubernetes.impl.visitor;

import io.fabric8.kubernetes.api.builder.v2_6.VisitableBuilder;
import io.fabric8.kubernetes.api.builder.v2_6.Visitor;
import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.clnt.v2_6.HasMetadataVisitiableBuilder;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NamespaceVisitor implements Visitor {

    @Inject
    Instance<Configuration> configuration;

    @Override
    public void visit(Object element) {
        new ImmutableNamespaceVisitor(configuration.get()).visit(element);
    }

    public static class ImmutableNamespaceVisitor implements Visitor {

        private final Configuration configuration;

        public ImmutableNamespaceVisitor(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void visit(Object element) {
            try {
                Method m = element.getClass().getMethod("withNamespace", String.class);
                m.invoke(element, configuration.getNamespace());
            } catch (NoSuchMethodException e) {
                //ignore this.
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
