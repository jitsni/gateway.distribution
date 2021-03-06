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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpSubjectSecurityFilter;
import org.kaazing.gateway.transport.http.util.Expectations;

public class DefaultAuthenticationTokenExtractorTest {
    private AuthenticationToken token;
    private AuthenticationTokenExtractor extractor = DefaultAuthenticationTokenExtractor.INSTANCE;;



    // see http://jira.kaazing.wan/browse/KG-4635
    @Test
    public void shouldSetAuthenticationTokenBasicScheme()
        throws Exception {

        URI uri = new URI("ws://localhost:8001/echo");
        String scheme = "Basic";
        String param = "key=value";
        String authorization = String.format("%s %s", scheme, param);

        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address, scheme));

        HttpRequestMessage request = new HttpRequestMessage();
        request.setMethod(HttpMethod.GET);
        request.setRequestURI(uri);
        request.setHeader(HttpSubjectSecurityFilter.AUTHORIZATION_HEADER, authorization);
        request.setLocalAddress(address);

        token = extractor.extract(request);
        String challengeScheme = token.getScheme();

        assertTrue(String.format("Expected challenge scheme '%s', got null", scheme), challengeScheme != null);
        assertTrue(String.format("Expected challenge scheme '%s', got '%s'", scheme, challengeScheme), challengeScheme.equals(scheme));
        assertEquals(1, token.size());
        assertEquals(param, token.get());
        assertEquals(param, token.get(0));
        assertNull(token.get("foo"));
        assertNull(token.get("key"));

        context.assertIsSatisfied();
    }

    // see http://jira.kaazing.wan/browse/KG-4635
    @Test
    public void shouldSetAuthenticationTokenApplicationBasicScheme()
        throws Exception {

        URI uri = new URI("ws://localhost:8001/echo");
        String scheme = "Application Basic";
        String param = "key=value";
        String authorization = String.format("%s %s", scheme, param);

        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address, scheme));

        HttpRequestMessage request = new HttpRequestMessage();
        request.setMethod(HttpMethod.GET);
        request.setRequestURI(uri);
        request.setHeader(HttpSubjectSecurityFilter.AUTHORIZATION_HEADER, authorization);
        request.setLocalAddress(address);

        token = extractor.extract(request);
        String challengeScheme = token.getScheme();

        assertTrue(String.format("Expected challenge scheme 'Application', got null", scheme), challengeScheme != null);
        assertTrue(String.format("Expected challenge scheme 'Application', got '%s'", scheme, challengeScheme), challengeScheme.equals("Application"));
        assertEquals(1, token.size());
        assertEquals(String.format("Basic %s", param), token.get());
        assertEquals(String.format("Basic %s", param), token.get(0));
        assertNull(token.get("foo"));
        assertNull(token.get("key"));

        context.assertIsSatisfied();
    }

    // see http://jira.kaazing.wan/browse/KG-4635
    @Test
    public void shouldSetAuthenticationTokenBasicSchemeWithSpaces()
        throws Exception {

        URI uri = new URI("ws://localhost:8001/echo");
        String scheme = "   Basic  ";
        String param = "key=value";
        String authorization = String.format("%s %s", scheme, param);

        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address, scheme));

        HttpRequestMessage request = new HttpRequestMessage();
        request.setMethod(HttpMethod.GET);
        request.setRequestURI(uri);
        request.setHeader(HttpSubjectSecurityFilter.AUTHORIZATION_HEADER, authorization);
        request.setLocalAddress(address);

        token = extractor.extract(request);
        String challengeScheme = token.getScheme();

        assertTrue(String.format("Expected challenge scheme '%s', got null", scheme), challengeScheme != null);
        assertTrue(String.format("Expected challenge scheme '%s', got '%s'", "Basic", challengeScheme), challengeScheme.equals("Basic"));
        assertEquals(1, token.size());
        assertEquals(String.format("  %s", param), token.get());
        assertEquals(String.format("  %s", param), token.get(0));
        assertNull(token.get("foo"));
        assertNull(token.get("key"));

        context.assertIsSatisfied();
    }

    // see http://jira.kaazing.wan/browse/KG-4635
    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldSetAuthenticationTokenBasicSchemeWithoutParams()
        throws Exception {

        URI uri = new URI("ws://localhost:8001/echo");
        String scheme = "Basic";
        String authorization = String.format("%s ", scheme);

        Mockery context = new Mockery();
        context.setImposteriser(ClassImposteriser.INSTANCE);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        context.checking(getExpectations(address, scheme));

        HttpRequestMessage request = new HttpRequestMessage();
        request.setMethod(HttpMethod.GET);
        request.setRequestURI(uri);
        request.setHeader(HttpSubjectSecurityFilter.AUTHORIZATION_HEADER, authorization);
        request.setLocalAddress(address);

        token = extractor.extract(request);
        String challengeScheme = token.getScheme();

        assertTrue(String.format("Expected challenge scheme '%s', got null", scheme), challengeScheme != null);
        assertTrue(String.format("Expected challenge scheme '%s', got '%s'", scheme, challengeScheme), challengeScheme.equals(scheme));
        assertNull(token.get());
        assertNull(token.get(0));
        assertNull(token.get("foo"));
        assertNull(token.get("key"));

        context.assertIsSatisfied();
    }


    private Expectations getExpectations(final ResourceAddress address,
                                         final String challengeScheme) {
        return new Expectations() {{
            allowing(address).getOption(HttpResourceAddress.REALM_AUTHENTICATION_HEADER_NAMES);
            will(returnValue(null));

            allowing(address).getOption(HttpResourceAddress.REALM_AUTHENTICATION_PARAMETER_NAMES);
            will(returnValue(null));

            allowing(address).getOption(HttpResourceAddress.REALM_AUTHENTICATION_COOKIE_NAMES);
            will(returnValue(null));

            allowing(address).getOption(HttpResourceAddress.REALM_AUTHORIZATION_MODE);
            will(returnValue(null));

            allowing(address).getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME);
            will(returnValue(challengeScheme));
        }
        };
    }
}
