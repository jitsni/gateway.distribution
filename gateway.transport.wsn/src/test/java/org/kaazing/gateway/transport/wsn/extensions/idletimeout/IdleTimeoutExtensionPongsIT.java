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

package org.kaazing.gateway.transport.wsn.extensions.idletimeout;

import static org.junit.rules.RuleChain.outerRule;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class IdleTimeoutExtensionPongsIT {

    private RobotRule robot = new RobotRule();

    private static final boolean ENABLE_DIAGNOSTICS = false;
    @BeforeClass
    public static void init()
            throws Exception {
        if (ENABLE_DIAGNOSTICS) {
            PropertyConfigurator.configure("src/test/resources/log4j-diagnostic.properties");
        }
    }

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .accept(URI.create("wsn://localhost:8001/proxy"))
                        .connect(URI.create("tcp://localhost:8002"))
                        .type("proxy")

                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                        .acceptOption("ws.inactivityTimeout", "2000milliseconds")
                    .done()
                .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(robot).around(gateway);

    @Robotic(script = "shouldGetPongsFromidleTimeoutExtension")
    @Test(timeout = 8 * 1000) 
    public void shouldGetPongsFromidleTimeoutExtensionWhenWriterIdle() throws Exception {
        Thread sleepingSocketRunner = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("### Creating server socket and listening");
                    ServerSocket listen = new ServerSocket();
                    listen.setReuseAddress(true);
                    listen.bind(new InetSocketAddress("localhost", 61234));
                    System.out.println("### Accepting connection");
                    Socket socket = listen.accept();
                    System.out.println("### Connection accepted");
                    for(int i=1; i<=8; i++) { // There are 8 sleeps in the robot scripts that need to be controlled, 300 ms apart
                        Thread.sleep(300); // 8*300 = 2400ms, just to cover the "ws.inactivityTimeout" = 2000ms
                        System.out.println("### Writing data to connection");
                        socket.getOutputStream().write(("WakeUp" + i).getBytes());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        });
        sleepingSocketRunner.start();
        robot.join();
    }

}
