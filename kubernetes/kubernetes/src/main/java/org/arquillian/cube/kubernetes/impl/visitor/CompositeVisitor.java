package org.arquillian.cube.kubernetes.impl.visitor;

import io.fabric8.kubernetes.api.builder.v2_6.TypedVisitor;
import io.fabric8.kubernetes.api.builder.v2_6.Visitor;
import java.lang.reflect.Method;
import java.util.List;

public class CompositeVisitor<T> implements Visitor<T> {

    private final List<Visitor<T>> visitors;

    public CompositeVisitor(List<Visitor<T>> visitors) {
        this.visitors = visitors;
    }

    private static <V, F> Boolean canVisit(V visitor, F fluent) {
        if (visitor instanceof TypedVisitor) {
            return ((TypedVisitor) visitor).getType().isAssignableFrom(fluent.getClass());
        }
        for (Method method : visitor.getClass().getDeclaredMethods()) {
            if (method.getParameterTypes().length != 1) {
                continue;
            }
            Class visitorType = method.getParameterTypes()[0];
            if (visitorType.isAssignableFrom(fluent.getClass())) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public void visit(T t) {
        for (Visitor delegate : visitors) {
            if (canVisit(delegate, t)) {
                delegate.visit(t);
            }
        }
    }
}
