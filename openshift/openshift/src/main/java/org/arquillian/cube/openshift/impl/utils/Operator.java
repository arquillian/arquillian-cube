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

/**
 * Operator
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public enum Operator {
    LESS_THAN(new Op() {
        public boolean op(int a, int b) {
            return a < b;
        }
    }),
    LESS_THAN_OR_EQUAL(new Op() {
        public boolean op(int a, int b) {
            return a <= b;
        }
    }),
    GREATER_THAN(new Op() {
        public boolean op(int a, int b) {
            return a > b;
        }
    }),
    GREATER_THAN_OR_EQUAL(new Op() {
        public boolean op(int a, int b) {
            return a >= b;
        }
    }),
    EQUAL(new Op() {
        public boolean op(int a, int b) {
            return a == b;
        }
    }),
    NOT_EQUAL(new Op() {
        public boolean op(int a, int b) {
            return a != b;
        }
    });

    private Op op;

    Operator(Op op) {
        this.op = op;
    }

    public boolean op(int a, int b) {
        return op.op(a, b);
    }

    private interface Op {
        boolean op(int a, int b);
    }
}
