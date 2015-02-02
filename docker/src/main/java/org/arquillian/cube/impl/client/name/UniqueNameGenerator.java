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


import java.util.UUID;



/**
 * Generates the name from a prefix, name, and assigns a timeuuid for unique
 */
public class UniqueNameGenerator implements  NameGenerator {


    public static final String TAG = "unique";

    private static final String DELIM = "_";


    private String prefix;


    public UniqueNameGenerator( final String prefix ) {


        if ( prefix == null ) {
            throw new IllegalArgumentException(
                    "You must specify a name generator prefix when using the unique name generator " );
        }

        this.prefix = prefix;
    }


    @Override
    public String getName( final String assignedName ) {
        return prefix + DELIM + assignedName + DELIM + UUID.randomUUID();
    }




}
