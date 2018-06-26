package org.arquillian.cube.kubernetes.impl.visitor;

import io.fabric8.kubernetes.api.builder.v4_0.TypedVisitor;
import io.fabric8.kubernetes.api.model.v4_0.HasMetadata;
import org.arquillian.cube.kubernetes.api.Logger;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class LoggingVisitor extends TypedVisitor<HasMetadata> {

    @Inject
    Instance<Logger> logger;

    @Override
    public void visit(HasMetadata item) {
        logger.get().info("Applying " + item.getKind() + ":" + item.getMetadata().getName());
    }
}
