/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.http.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;

public class ThrowableContainer {

    private Collection<Throwable> throwables;

    public ThrowableContainer() {
        this.throwables = new LinkedHashSet<Throwable>();
    }

    public void add(Throwable t) {
        if ( !throwables.contains(t) ) {
            throwables.add(t);
        }
    }

    public boolean isEmpty() {
        return throwables.isEmpty();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return super.toString();
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(out);
            StringBuilder sb = new StringBuilder();
            sb.append("Throwables encountered: {\n");
            for ( Throwable t: throwables) {
                t.fillInStackTrace();
                t.printStackTrace(ps);
                ps.println("\n---------------\n");
            }
            sb.append("\n}\n").append(out.toString());
            return sb.toString();
        }
    }

}
