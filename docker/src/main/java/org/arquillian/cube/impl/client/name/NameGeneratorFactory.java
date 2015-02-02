/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */
package org.arquillian.cube.impl.client.name;


import org.arquillian.cube.impl.client.CubeConfiguration;


/**
 * Return the name generator impl based on the configuration
 */
public class NameGeneratorFactory {

    /**
     * Get the name generator for the configured option
     *
     * @param cubeConfiguration The cube configuration
     *
     * @return The NameGenerator instance to use when generating names.
     */
    public static NameGenerator getGenerator( final CubeConfiguration cubeConfiguration ) {

        final String configurationName = cubeConfiguration.getNameGenerator();

        //can't switch on a null
        if ( configurationName == null ) {
            return new StaticNameGenerator();
        }

        switch ( configurationName ) {
            case StaticNameGenerator.TAG:
                return new StaticNameGenerator();
            case UniqueNameGenerator.TAG:
                return new UniqueNameGenerator( cubeConfiguration.getGetNameGeneratorPrefix() );
            default:
                throw new IllegalArgumentException(
                        "The configuration of type '" + configurationName + "' is not a valid configuration.  Use '"
                                + StaticNameGenerator.TAG + "' or '" + UniqueNameGenerator.TAG + "'." );
        }
    }
}
