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

package org.kaazing.gateway.service.proxy;

import java.net.URI;
import java.util.Collection;

import javax.annotation.Resource;

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.BridgeServiceFactory;

/**
 * Gateway service of type "proxy".
 */
public class ProxyService extends AbstractProxyService<ProxyServiceHandler> {
    private BridgeServiceFactory bridgeServiceFactory;

    public ProxyService() {
    }

    @Override
    public String getType() {
        return "proxy";
    }

    @Override
    protected ProxyServiceHandler createHandler() {
        return new ProxyServiceHandler();
    }

    @Resource(name = "bridgeServiceFactory")
    public void setBridgeServiceFactory(BridgeServiceFactory bridgeServiceFactory) {
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        super.init(serviceContext);
        Collection<URI> connectURIs = serviceContext.getConnects();
        if (connectURIs == null || connectURIs.isEmpty()) {
            throw new IllegalArgumentException("Missing required element: <connect>");
        }
        ProxyServiceHandler handler = getHandler();
        handler.setConnectURIs(connectURIs);
        handler.initServiceConnectManager(bridgeServiceFactory);

        // Register the Gateway's connection capabilities with the handler so that session counts are tracked
    }

    @Override
    public void start() throws Exception {
        super.start();
        getHandler().startServiceConnectManager();
    }

    // FIXME:  How should this be exposed to Management?  For now the service connect manager object is exposed through this method, but
    //         perhaps management could attach a listener that in turn gets passed to the handler and on to the connect manager...
    public ServiceConnectManager getServiceConnectManager() {
        return getHandler().getServiceConnectManager();
    }
}
