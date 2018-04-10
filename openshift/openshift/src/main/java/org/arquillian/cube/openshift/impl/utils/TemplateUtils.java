/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.arquillian.cube.openshift.impl.utils;

import org.arquillian.cube.kubernetes.impl.resolver.ResourceResolver;
import org.arquillian.cube.openshift.api.Replicas;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Template utils.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TemplateUtils {

    @SuppressWarnings({"rawtypes", "unchecked" })
    public static void addParameterValues(List<ParamValue> values, Map map, boolean filter) {
        Set<Map.Entry> entries = map.entrySet();
        for (Map.Entry env : entries) {
            if (env.getKey() instanceof String && env.getValue() instanceof String) {
                String key = (String) env.getKey();
                if (filter == false || key.startsWith("ARQ_") || key.startsWith("arq_")) {
                    if (filter) {
                        values.add(new ParamValue(key.substring("ARQ_".length()), (String) env.getValue()));
                    } else {
                        values.add(new ParamValue(key, (String) env.getValue()));
                    }
                }
            }
        }
    }

    public static String readTemplateUrl(Template template, CubeOpenShiftConfiguration configuration,
        boolean required, StringResolver resolver) {
        String templateUrl = template == null ? null : template.url();
        if (templateUrl == null || templateUrl.length() == 0) {
            templateUrl = configuration.getTemplateURL();
        }

        if (templateUrl == null && required) {
            throw new IllegalArgumentException("Missing template URL! Either add @Template to your test or add -Dopenshift.template.url=<url>");
        }

        if (templateUrl != null) {
            String url = resolver.resolve(templateUrl);
            templateUrl = ResourceResolver.resolve(url).toString();
        }

        return templateUrl;
    }

    public static <T> int readReplicas(T type) {
        Replicas replicas = null;
        if (type instanceof Method) {
            replicas = ((Method) type).getAnnotation(Replicas.class);
        } else if (type instanceof Class) {
            replicas = (Replicas) ((Class) type).getAnnotation(Replicas.class);
        }
        int r = 1;
        if (replicas != null) {
            if (replicas.value() <= 0) {
                throw new IllegalArgumentException("Non-positive replicas size: " + replicas.value());
            }
            r = replicas.value();
        }

        return r;
    }

    public static Map<String, String> readLabels(Template template, CubeOpenShiftConfiguration configuration, StringResolver resolver) {
        if (template != null) {
            String string = template.labels();
            if (string != null && string.length() > 0) {
                Map<String, String> map = Strings.splitKeyValueList(string);
                Map<String, String> resolved = new HashMap<>();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    resolved.put(resolver.resolve(entry.getKey()), resolver.resolve(entry.getValue()));
                }
                return resolved;
            }
        }
        return configuration.getTemplateLabelsAsMap();
    }

    public static boolean executeProcessTemplate(Template template, CubeOpenShiftConfiguration configuration) {
        return (template == null || template.process()) && configuration.isTemplateProcess();
    }

    public static Map<String, String> readParameters(Template template, CubeOpenShiftConfiguration configuration, StringResolver resolver) {
        if (template != null) {
            TemplateParameter[] parameters = template.parameters();
            Map<String, String> map = new HashMap<>();
            for (TemplateParameter parameter : parameters) {
                String name = resolver.resolve(parameter.name());
                String value = resolver.resolve(parameter.value());
                map.put(name, value);
            }
            return map;
        }
        return configuration.getTemplateParametersAsMap();
    }
}
