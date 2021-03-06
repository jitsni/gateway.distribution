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

package org.kaazing.gateway.transport.http.security.auth.token;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.util.Expectations;

public class CustomAuthenticationTokenParametersTest {


    AuthenticationTokenExtractor extractor = DefaultAuthenticationTokenExtractor.INSTANCE;

    @Test
    public void testAttemptToExtractParamButParamMissing() throws Exception {

        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address, "Basic", null, null, null, "challenge"));

        HttpRequestMessage requestMessage = new HttpRequestMessage();
        requestMessage.setLocalAddress(address);
        DefaultAuthenticationToken token = (DefaultAuthenticationToken) extractor.extract(requestMessage);
        assertEquals("Expecting empty token", 0, token.size());

        context.assertIsSatisfied();
    }

    @Test
    public void testAttemptToExtractParamWithSingleValue() throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address, "Basic", null, new String[]{"foo"}, null, "challenge"));

        HttpRequestMessage requestMessage = new HttpRequestMessage();
        requestMessage.setLocalAddress(address);
        requestMessage.setParameter("foo", "bar");
        DefaultAuthenticationToken token = (DefaultAuthenticationToken) extractor.extract(requestMessage);
        assertEquals("Expecting single sized token", 1, token.size());
        assertEquals("Expecting value 'bar'", "bar", token.get());
        assertEquals("Expecting value 'bar'", "bar", token.get("foo"));

        context.assertIsSatisfied();
    }

    @Test
    public void testAttemptToExtractParamWithMultipleValues() throws Exception {
        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address, "Basic", null, new String[]{"foo"}, null, "challenge"));

        HttpRequestMessage requestMessage = new HttpRequestMessage();
        requestMessage.setLocalAddress(address);
        Map<String, List<String>> newParams = new HashMap<String, List<String>>();
        List<String> newParamValues = new ArrayList<String>();
        newParamValues.add("bar");
        newParamValues.add("baz");
        newParams.put("foo", newParamValues);
        requestMessage.setParameters(newParams);
        DefaultAuthenticationToken token = (DefaultAuthenticationToken) extractor.extract(requestMessage);
        assertEquals("Expecting single sized token", 1, token.size());
        assertEquals("Expecting value 'bar'", "bar", token.get("foo"));

        context.assertIsSatisfied();
    }


    private Expectations getExpectations(final ResourceAddress address,
                                         final String challengeScheme, final String[] headerNames,
                                         final String[] parameterNames,
                                         final String[] cookieNames,
                                         final String authorizationMode) {
        return new Expectations() {{
            allowing(address).getOption(HttpResourceAddress.REALM_AUTHENTICATION_HEADER_NAMES);
            will(returnValue(headerNames));

            allowing(address).getOption(HttpResourceAddress.REALM_AUTHENTICATION_PARAMETER_NAMES);
            will(returnValue(parameterNames));

            allowing(address).getOption(HttpResourceAddress.REALM_AUTHENTICATION_COOKIE_NAMES);
            will(returnValue(cookieNames));

            allowing(address).getOption(HttpResourceAddress.REALM_AUTHORIZATION_MODE);
            will(returnValue(authorizationMode));

            allowing(address).getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME);
            will(returnValue(challengeScheme));
        }
        };
    }


}
