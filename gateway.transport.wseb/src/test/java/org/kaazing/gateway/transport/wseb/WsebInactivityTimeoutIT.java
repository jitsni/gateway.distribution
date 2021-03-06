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

package org.kaazing.gateway.transport.wseb;

import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class WsebInactivityTimeoutIT {

    private final RobotRule robot = new RobotRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .tempDirectory(new File("src/test/temp"))
                            /*
                        .service()
                            .accept(URI.create("http://localhost:8123/"))
                            .type("directory")
                            .property("directory", "/extras")
                        .done()
                            */
                        .service()
                            .accept(URI.create("wse://localhost:8123/echo"))
                            // NOTE: even though in the config file it's ws.inactivity.timeout!
                            .acceptOption("ws.inactivityTimeout", "2sec")
                            .type("echo")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(robot).around(gateway);

    @Robotic("echo.inactivity.timeout.should.close")
    @Test(timeout = 15000)
    public void testEchoInactiveTimeoutShouldCloseConnection() throws Exception {
        robot.join();
    }

    @Robotic("echo.inactivity.timeout.should.not.ping.old.client")
    @Test(timeout = 15000)
    public void testEchoInactiveTimeoutShouldNotPingOldClient() throws Exception {
        robot.join();
    }

}
