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
import java.net.URI;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class HttpDirectoryServiceIT {

    private static String DIRECTORY_SERVICE_ACCEPT = "http://localhost:8000/";
    private static String CROSS_ORIGIN_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8001/";
    private static String ASTRISK_ORIGIN_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8002/";
    private static String KEEPALIVE_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8003/keepAlive";

    private final RobotRule robot = new RobotRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                            .service()
                                .accept(URI.create(KEEPALIVE_DIRECTORY_SERVICE_ACCEPT))
                                .type("directory")
                                .property("directory", "/public")
                                // We have to use this name (which is from TransportOptionNames) instead of "http.keepalive.timeout",
                                // see Gateway.camelCaseToDottedLowerCase.
                                .acceptOption("http.keepaliveTimeout", "3") // seconds
                        .done()
                        .service()
                            .accept(URI.create(DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                        .done()
                        .service()
                            .accept(URI.create(CROSS_ORIGIN_DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                                .crossOrigin().allowOrigin("http://localhost:8000")
                                .allowHeaders("x-websocket-protocol").allowMethods("GET")
                            .done()
                        .done()
                        .service()
                            .accept(URI.create(ASTRISK_ORIGIN_DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                                .crossOrigin().allowOrigin("*")
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(robot).around(gateway);

    @Robotic(script = "get.index.check.status.code.200")
    @Test(timeout = 8000)
    public void testGetIndexAndStatusCode200() throws Exception {
        robot.join();
    }

    @Robotic(script = "get.nonexistent.page.check.status.code.404")
    @Test(timeout = 8000)
    public void testGetNonexistantPageCheckStatusCode404() throws Exception {
        robot.join();
    }

    @Robotic(script = "get.nonexistent.page.check.http.keepalive.timeout")
    @Test(timeout = 8000)
    // keepalive timeout is set at 3 secs so this should suffice to see the server disconnect
    public void testGetNonexistantPageCheckConnectionTimesOut() throws Exception {
        robot.join();
    }

    @Robotic(script = "tcp.connect.and.close.to.directory.service")
    @Test(timeout = 8000)
    public void testTcpConnectAndClose() throws Exception {
        robot.join();
    }

    /**
     * BUG for KG-7642 TODO @Ignore particular test in JIRA
     */
    @Ignore
    @Robotic(script = "tcp.connect.and.wait.for.close.to.directory.service")
    @Test(timeout = 35000)
    public void testTcpConnectAndWaitForClose() throws Exception {
        robot.join();
    }

    @Robotic(script = "get.index.check.whole.response")
    @Test(timeout = 8000)
    public void testGetIndexCheckWholeResponse() throws Exception {
        robot.join();
    }

    @Robotic(script = "post.large.data")
    @Test(timeout = 25000)
    public void testPostLargeData() throws Exception {
        robot.join();
    }

    @Robotic(script = "get.forbidden.file.exists")
    @Test(timeout = 5000)
    public void testGetForbiddenFileExists() throws Exception {
        robot.join();
    }

    // /////////////////// HOST HEADER ///////////////////////
    @Robotic(script = "host/host.empty.header.with.absolute.uri")
    @Test(timeout = 5000)
    public void testEmptyHostHeaderWithAbsoluteUri() throws Exception {
        robot.join();
    }

    @Robotic(script = "host/host.empty.header.with.relative.uri")
    @Test(timeout = 5000)
    public void testEmptyHostHeaderWithRelativeUri() throws Exception {
        robot.join();
    }

    @Robotic(script = "host/host.header.invalid")
    @Test(timeout = 5000)
    public void testInvalidHostHeader() throws Exception {
        robot.join();
    }

    @Robotic(script = "host/host.header.not.present")
    @Test(timeout = 5000)
    public void testAbsentHostHeader() throws Exception {
        robot.join();
    }

    @Robotic(script = "host/host.header.port.not.set")
    @Test(timeout = 5000)
    public void testAbsentPortInHostHeader() throws Exception {
        robot.join();
    }

    @Robotic(script = "host/host.valid.header.with.absolute.uri")
    @Test(timeout = 5000)
    public void testValidHostHeaderWithAbsoluteUri() throws Exception {
        robot.join();
    }

    @Robotic(script = "host/host.no.header.with.absolute.uri")
    @Test(timeout = 5000)
    public void testNoHostHeaderWithAbsoluteUri() throws Exception {
        robot.join();
    }

    // ///////////////////// METHOD //////////////////////
    @Robotic(script = "method/method.connect")
    @Test(timeout = 5000)
    public void testConnectMethod() throws Exception {
        robot.join();
    }

    @Robotic(script = "method/method.delete")
    @Test(timeout = 5000)
    public void testDeleteMethod() throws Exception {
        robot.join();
    }

    @Robotic(script = "method/method.head")
    @Test(timeout = 5000)
    public void testHeadMethod() throws Exception {
        robot.join();
    }

    @Robotic(script = "method/method.options")
    @Test(timeout = 5000)
    public void testOptionsMethod() throws Exception {
        robot.join();
    }

    @Robotic(script = "method/method.bogus")
    @Test(timeout = 5000)
    public void testBogusMethod() throws Exception {
        robot.join();
    }

    @Robotic(script = "method/method.post")
    @Test(timeout = 5000)
    public void testPostMethod() throws Exception {
        robot.join();
    }

    // //////////////// URI ////////////////////////
    @Robotic(script = "uri.hex.encoded")
    @Test(timeout = 5000)
    public void testHexEncodedUri() throws Exception {
        robot.join();
    }

    @Robotic(script = "uri.too.long")
    @Test(timeout = 5000)
    public void testUriTooLong() throws Exception {
        robot.join();
    }

    @Robotic(script = "invalid.uri.with.space")
    @Test(timeout = 5000)
    public void testInvalidUri() throws Exception {
        robot.join();
    }

    @Robotic(script = "uri.with.params")
    @Test(timeout = 5000)
    public void testUriWithParams() throws Exception {
        robot.join();
    }

    // ////////////////// CROSS-ORIGIN ACCESS ////////////////////

    @Robotic(script = "origin/same.origin.constraint.not.set")
    @Test(timeout = 5000)
    public void testSameOriginConstraintNotSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/different.origin.constraint.not.set")
    @Test(timeout = 5000)
    public void testDifferentOriginConstraintNotSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/missing.origin.header.constraint.not.set")
    @Test(timeout = 5000)
    public void testMissingOriginHeaderConstraintNotSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/wrong.host.constraint.set")
    @Test(timeout = 5000)
    public void testWrongHostConstraintSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/wrong.port.constraint.set")
    @Test(timeout = 5000)
    public void testWrongPortConstraintSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/missing.origin.header.constraint.set")
    @Test(timeout = 5000)
    public void testMissingOriginHeaderConstraintSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/empty.origin.header.constraint.set")
    @Test(timeout = 5000)
    public void testEmptyOriginHeaderConstraintSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/empty.origin.header.constraint.not.set")
    @Test(timeout = 5000)
    public void testEmptyOriginHeaderConstraintNotSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/different.origin.constraint.asterisk")
    @Test(timeout = 5000)
    public void testDifferentOriginConstraintAsterisk() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/empty.origin.header.constraint.asterisk")
    @Test(timeout = 5000)
    public void testEmptyOriginHeaderConstraintAsterisk() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/missing.origin.header.constraint.asterisk")
    @Test(timeout = 5000)
    public void testMissingOriginHeaderConstraintAsterisk() throws Exception {
        robot.join();
    }


    @Robotic(script = "origin/not.allowed.method")
    @Test(timeout = 5000)
    public void testNotAllowedMethodConstraintSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/not.allowed.header")
    @Test(timeout = 5000)
    public void testNotAllowedHeaderConstraintSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/allowed.method")
    @Test(timeout = 5000)
    public void testAllowedMethodConstraintSet() throws Exception {
        robot.join();
    }

    @Robotic(script = "origin/allowed.header")
    @Test(timeout = 5000)
    public void testAllowedHeaderConstraintSet() throws Exception {
        robot.join();
    }

}
