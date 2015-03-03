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

package org.kaazing.gateway.management;

import java.net.URI;
import java.util.Collection;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;

/**
 * Interface to be implemented by those objects that want to act as listeners for cluster-level management events. Presumably
 * each implementer of this interface would be protocol-specific.
 */
public interface ClusterManagementListener {

    void setGatewayBean(GatewayManagementBean gatewayBean);

    void membershipChanged(String changeType, String instanceKey);

    void managementServicesChanged(String changeType, String instanceKey, Collection<URI> managementServiceAccepts);

    void balancerMapChanged(String changeType, URI balancerURI, Collection<URI> balanceeURIs);

}
