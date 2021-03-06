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

package org.kaazing.gateway.transport.http.bridge.filter;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;

public class HttpNextProtocolHeaderFilter extends HttpFilterAdapter<IoSession> {

    public static final String PROTOCOL_HTTPXE_1_1 = "httpxe/1.1";

    private static final String HEADER_X_NEXT_PROTOCOL = "X-Next-Protocol";

    private static final String QUERY_PARAM_NEXT_PROTOCOL = ".knp";

    private static final String WEB_SOCKET = "WebSocket";

    private static final String HEADER_WEBSOCKET_KEY = "Sec-WebSocket-Key";

    @Override
    protected void httpRequestReceived(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest)
            throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        String upgrade = httpRequest.getHeader("Upgrade");

        if (upgrade != null &&
                WEB_SOCKET.equalsIgnoreCase(upgrade)) {
            if (httpRequest.getHeader(HEADER_WEBSOCKET_KEY) != null) {
                httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, "ws/rfc6455");
            }  else if ( httpRequest.getHeader(HEADER_WEBSOCKET_KEY) == null) {
                httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, "ws/draft-7x");
            }
        }
        String nextProtocolHeader = httpRequest.getHeader(HEADER_X_NEXT_PROTOCOL);
        if (nextProtocolHeader == null) {
            // allow use of next-protocol query parameter for IFrame streaming (no custom HTTP headers possible)
            nextProtocolHeader = httpRequest.removeParameter(QUERY_PARAM_NEXT_PROTOCOL);
            // promote to header for consistent consumption by HTTP transport
            if (nextProtocolHeader != null) {
                httpRequest.setHeader(HEADER_X_NEXT_PROTOCOL, nextProtocolHeader);
            }
        }
        
        super.httpRequestReceived(nextFilter, session, httpRequest);
    }

}
