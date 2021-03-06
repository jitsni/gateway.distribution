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

package org.kaazing.gateway.service.http.directory;

import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class HttpDirectoryServiceAuthorizationIT {

    private static String DIRECTORY_SERVICE_ACCEPT = "http://localhost:8000/";
    private static String BASIC_AUTH_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8008/auth";
    private static String APP_BASIC_AUTH_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8009/auth";
    private static String TOKEN_AUTH_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8010/auth";
    
    private final RobotRule robot = new RobotRule();

    private final GatewayRule gateway = new GatewayRule() {
        {

            KeyStore keyStore = null;
            try {
                FileInputStream fileInStr = new FileInputStream(System.getProperty("user.dir")
                        + "/target/truststore/keystore.db");
                keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(fileInStr, "ab987c".toCharArray());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .property(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY, "src/test/resources/gateway/conf")
                        .service()
                            .accept(URI.create(DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                        .done()
                        .service()
                            .accept(URI.create(BASIC_AUTH_DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                            .realmName("basic")
                            .authorization()
                                .requireRole("AUTHORIZED")
                            .done()
                        .done()
                        .service()
                            .accept(URI.create(APP_BASIC_AUTH_DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                            .realmName("application-basic")
                            .authorization()
                                .requireRole("AUTHORIZED")
                            .done()
                        .done()
                        .service()
                            .accept(URI.create(TOKEN_AUTH_DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                            .realmName("token")
                            .authorization()
                                .requireRole("AUTHORIZED")
                            .done()
                        .done()
                        .security()
                            .keyStore(keyStore)
                            .realm()
                                .name("token")
                                .description("Kaazing WebSocket Gateway Demo")
                                .httpChallengeScheme("Application Token")   
                                .authorizationMode("challenge")
                                .loginModule()                          
                                    .type("class:org.kaazing.gateway.service.http.directory.TokenLoginModule")
                                    .success("requisite")
                                    .option("roles", "AUTHORIZED, ADMINISTRATOR")
                                .done()
                            .done()
                            .realm()
                                .name("basic")
                                .description("Basic Authentication")
                                .httpChallengeScheme("Basic")
                                .authorizationMode("challenge")
                                .loginModule()
                                    .type("file")
                                    .success("required")
                                    .option("file", "jaas-config.xml")
                                .done()
                             .done()
                            .realm()
                                .name("application-basic")
                                .description("Application Basic Authentication")
                                .httpChallengeScheme("Application Basic")
                                .authorizationMode("challenge")
                                .loginModule()
                                    .type("file")
                                    .success("required")
                                    .option("file", "jaas-config.xml")
                                .done()
                             .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(robot).around(gateway);

    // //////////////////////// AUTHORIZATION //////////////////
    @Robotic(script = "auth/app.basic.authorized.access.with.valid.credentials")
    @Test(timeout = 500000)
    public void testAppBasicAuthorizedWithValidCredentials() throws Exception {
        robot.join();
    }

    @Robotic(script = "auth/app.basic.authorized.access.with.invalid.credentials")
    @Test(timeout = 5000)
    public void testAppBasicAuthorizedWithInvalidCredentials() throws Exception {
        robot.join();
    }

    @Robotic(script = "auth/app.basic.authorized.directory.access.no.credentials")
    @Test(timeout = 5000)
    public void testAppBasicAuthorizedWithoutCredentials() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "auth/basic.authorized.access.with.valid.credentials")
    @Test(timeout = 5000)
    public void testBasicAuthorizedWithValidCredentials() throws Exception {
        robot.join();
    }

    @Robotic(script = "auth/basic.authorized.access.with.invalid.credentials")
    @Test(timeout = 5000)
    public void testBasicAuthorizedWithInvalidCredentials() throws Exception {
        robot.join();
    }

    @Robotic(script = "auth/basic.authorized.directory.access.no.credentials")
    @Test(timeout = 5000)
    public void testBasicAuthorizedWithoutCredentials() throws Exception {
        robot.join();
    }

    @Robotic(script = "auth/token.authorized.access.with.valid.credentials")
    @Test(timeout = 5000)
    public void testTokenAuthorizedWithValidCredentials() throws Exception {
        robot.join();
    }

    @Robotic(script = "auth/token.authorized.access.with.invalid.credentials")
    @Test(timeout = 5000)
    public void testTokenAuthorizedWithInValidCredentials() throws Exception {
        robot.join();
    }

    @Robotic(script = "auth/token.authorized.directory.access.no.credentials")
    @Test(timeout = 5000)
    public void testTokenAuthorizedWithoutCredentials() throws Exception {
        robot.join();
    }
}
