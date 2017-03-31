/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl;

public class Constants {


    public static final String CLIENT_CREATOR_CLASS_NAME = "kubernetes.client.creator.class.name";

    // Non-config constants
    public static final String JAVA_PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
    public static final String PROTOCOL_HANDLERS = "protocolHandlers";
    public static final String DEFAULT_MAVEN_PROTOCOL_HANDLER = "org.ops4j.pax.url";


    public static final String RUNNING_STATUS = "RUNNING";
    public static final String ABORTED_STATUS = "ABORTED";
    public static final String ERROR_STATUS = "ERROR";
}
