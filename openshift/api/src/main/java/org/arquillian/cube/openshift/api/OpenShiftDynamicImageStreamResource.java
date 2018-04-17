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

package org.arquillian.cube.openshift.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to generate dynamically OpenShift image stream resource before test execution.
 *
 * @author <a href="mailto:mgoldman@redhat.com">Marek Goldmann</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Repeatable(OpenShiftDynamicImageStreamResource.List.class)
public @interface OpenShiftDynamicImageStreamResource {
    /**
     * Name of the image stream. This is important to use the same name
     * as defined in template later.
     *
     * @return Name of the image stream
     */
    String name();

    /**
     * Full image name (including registry and tag)
     *
     * @return full image name, with tag
     */
    String image();

    /**
     * Version of the image stream
     *
     * @return
     */
    String version();

    /**
     * Defines if the image stream should be marked as insecure. Use
     * 'true' or 'false' strings. By default every image stream is marked
     * as insecure.
     *
     * @return string representation of boolean name
     */
    String insecure() default "true";

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
    @interface List {
        OpenShiftDynamicImageStreamResource[] value();
    }
}
