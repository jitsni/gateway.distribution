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

package org.kaazing.gateway.resource.address.ws;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.BIND_ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.CODEC_REQUIRED;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.EXTENSIONS;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.LIGHTWEIGHT;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.MAX_MESSAGE_SIZE;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.REQUIRED_PROTOCOLS;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.SUPPORTED_PROTOCOLS;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.Comparators;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;

public class WsResourceAddressFactorySpiTest {
    private final ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

    private WsResourceAddressFactorySpi addressFactorySpi;
    private URI addressURI;
    private Map<String, Object> options;
    
    @Before
    public void before() {
        addressFactorySpi = new WsResourceAddressFactorySpi();
        addressURI = URI.create("ws://localhost:2020/");
        options = new HashMap<String, Object>();
        options.put("ws.nextProtocol", "custom");
        options.put("ws.qualifier", "random");
        options.put("ws.codecRequired", FALSE);
        options.put("ws.lightweight", TRUE);
        options.put("ws.extensions", asList("x-kaazing-alpha", "x-kaazing-beta"));
        options.put("ws.maxMessageSize", 1024);
        options.put("ws.inactivityTimeout", SECONDS.toMillis(5));
        options.put("ws.supportedProtocols", new String[] { "amqp/0.91", "amqp/1.0" });
        options.put("ws.requiredProtocols", new String[] { "amqp/0.91", "amqp/1.0" });
        options.put("ws.transport", URI.create("http://localhost:2121/"));
    }

    @Test
    public void shouldHaveWsSchemeName() throws Exception {
        assertEquals("ws", addressFactorySpi.getSchemeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireWsnSchemeName() throws Exception {
        addressFactorySpi.newResourceAddress(URI.create("test://opaque"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPath() throws Exception {
        addressFactorySpi.newResourceAddress(URI.create("ws://localhost:80"));
    }

    @Test 
    public void shouldNotRequireExplicitPort() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(URI.create("ws://localhost/"));
        URI location = address.getResource();
        assertEquals(location.getPort(), 80);
    }

    @Test
    public void shouldCreateAddressWithDefaultOptions() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertNull(address.getOption(TRANSPORT)); // we defined a transportFactory now.
        assertTrue(address.getOption(CODEC_REQUIRED));
        assertFalse(address.getOption(LIGHTWEIGHT));
        assertEmpty(address.getOption(EXTENSIONS));
        assertEquals(0, address.getOption(MAX_MESSAGE_SIZE).intValue());
        assertEquals(0L, address.getOption(INACTIVITY_TIMEOUT).longValue());
        assertEmpty(address.getOption(SUPPORTED_PROTOCOLS));
        assertEmpty(address.getOption(REQUIRED_PROTOCOLS));
    }

    @Test
    public void shouldCreateAddressWithOptions() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI, options);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertNull(address.getOption(TRANSPORT)); // we defined a transportFactory now.
        assertFalse(address.getOption(CODEC_REQUIRED));
        assertTrue(address.getOption(LIGHTWEIGHT));
        assertEquals(asList("x-kaazing-alpha", "x-kaazing-beta"), address.getOption(EXTENSIONS));
        assertEquals(1024, address.getOption(MAX_MESSAGE_SIZE).intValue());
        assertEquals(SECONDS.toMillis(5), address.getOption(INACTIVITY_TIMEOUT).longValue());
        assertArrayEquals(new String[] { "amqp/0.91", "amqp/1.0" }, address.getOption(SUPPORTED_PROTOCOLS));
        assertArrayEquals(new String[] { "amqp/0.91", "amqp/1.0" }, address.getOption(REQUIRED_PROTOCOLS));
    }

    @Test
    public void shouldCreateAddressWithDefaultTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals(URI.create("http://localhost:2020/"), address.getOption(TRANSPORT_URI));
    }
    
    @Test
    public void shouldCreateAddressWithTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI, options);
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals(URI.create("http://localhost:2121/"), address.getOption(TRANSPORT_URI));
    }

    @Test
    public void shouldCreateWsResourceAddressWithDefaultBindAlternate() {

        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        addressFactorySpi.setResourceAddressFactory(addressFactory);

        ResourceAddress address = addressFactory.newResourceAddress(addressURI);
        assertEquals(addressURI, address.getResource());
        // bind alternate should be false for all ws resource addresses... only while WsAcceptor
        // delegates the binds explicitly.  If we ever remove WsAcceptor this will change.
        assertFalse(address.getOption(BIND_ALTERNATE));
    }

    @Test
    public void testWsAlternates() throws Exception {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI);

        ResourceAddress wse = address.getOption(ALTERNATE);
        assertEquals("wse", wse.getExternalURI().getScheme());

        ResourceAddress wsx = wse.getOption(ALTERNATE);
        assertEquals("wsx", wsx.getExternalURI().getScheme());

        ResourceAddress wsdraft = wsx.getOption(ALTERNATE);
        assertEquals("ws-draft", wsdraft.getExternalURI().getScheme());

        ResourceAddress wsxdraft = wsdraft.getOption(ALTERNATE);
        assertEquals("wsx-draft", wsxdraft.getExternalURI().getScheme());

        ResourceAddress wsr = wsxdraft.getOption(ALTERNATE);
        assertEquals("wsr", wsr.getExternalURI().getScheme());

        assertNull(wsr.getOption(ALTERNATE));
    }

    @Test
    public void shouldCreateAddressWithCorrectAlternateAddresses() throws Exception {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI);
        assertNotNull(address.getOption(ALTERNATE));
        assertFalse(address.getOption(BIND_ALTERNATE));

        ResourceAddress cursor = address;

        int alternateCount = 0;


        while (cursor != null) {
            alternateCount++;

            switch(alternateCount) {
                case 1: verifyExpectedAddress(cursor, "ws://localhost:2020/", 3, "ws/rfc6455"); break;
                case 2: verifyExpectedAddress(cursor, "ws://localhost:2020/", 3, "wse/1.0"); break;
                // This could change to depth 4 once we make httpxe addresses primary.
                // case 2: verifyExpectedAddress(cursor, "ws://localhost:2020/", 4, "wse/1.0"); break;
                case 3: verifyExpectedAddress(cursor, "ws://localhost:2020/", 5, "ws/rfc6455"); break;
                case 4: verifyExpectedAddress(cursor, "ws://localhost:2020/", 3, "ws/draft-7x"); break;
                case 5: verifyExpectedAddress(cursor, "ws://localhost:2020/", 5, "ws/draft-7x"); break;
                case 6: verifyExpectedAddress(cursor, "ws://localhost:2020/", 3, "wsr/1.0"); break;
                // This could change to depth 4 once we make httpxe addresses primary.
                //case 6: verifyExpectedAddress(cursor, "ws://localhost:2020/", 4, "wsr/1.0"); break;
            }
            cursor = cursor.getOption(ALTERNATE);
        }

        assertEquals(6, alternateCount);

    }

    private void verifyExpectedAddress(ResourceAddress address, String location, int depth, String protocolName) {
        assertEquals(URI.create(location), address.getResource());
        int actualDepth = 0;
        ResourceAddress cursor = address;
        while(cursor != null) {
            actualDepth++;
            cursor = cursor.getTransport();
        }
        assertEquals(depth, actualDepth);
        assertEquals(protocolName, address.getTransport().getOption(NEXT_PROTOCOL));
        assertFalse(address.getOption(BIND_ALTERNATE));
    }

    private void assertEmpty(List<String> objects) {
        if (objects != null) {
            assertEquals(0, objects.size());
        }
    }

    private void assertEmpty(String[] objects) {
        if (objects != null) {
            assertEquals(0, objects.length);
        }
    }


    @Test
    public void testAlternateAddressComparison() throws Exception {
        Comparator<ResourceAddress> cmp = Comparators.compareResourceOriginPathAlternatesAndProtocolStack();
        ResourceAddress addr1 = makeResourceAddress(new URI("ws://localhost:8001/echo"));

        ResourceAddress addr2 = makeResourceAddress(new URI("wsr://localhost:8001/echo"));
        assertEquals(0, cmp.compare(addr1, addr2));

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("http.nextProtocol", "wse/1.0");
        ResourceAddress addr3 = addressFactory.newResourceAddress(new URI("wse://localhost:8001/echo"), options);
        assertEquals(0, cmp.compare(addr1, addr3));

        options = new HashMap<String, Object>();
        options.put("http.nextProtocol", "xxx/1.0");
        ResourceAddress addr4 = addressFactory.newResourceAddress(new URI("wse://localhost:8001/echo"), options);
        assertNotEquals(0, cmp.compare(addr1, addr4));

        options = new HashMap<String, Object>();
        options.put("http.transport", new URI("tcp://localhost:8002"));
        ResourceAddress addr5 = addressFactory.newResourceAddress(new URI("ws://localhost:8001/echo"), options);
        assertNotEquals(0, cmp.compare(addr1, addr5));
    }

    @Test
    public void testAlternateAddressComparisonInSkipList() throws Exception {
        Comparator<ResourceAddress> cmp = Comparators.compareResourceOriginPathAlternatesAndProtocolStack();

        List<Integer> ports = Arrays.asList(30, 40, 50, 60, 61, 62, 63, 70, 1024, 2045, 235, 345, 123, 456,
                3276, 9000, 3489, 9234, 765, 4567, 3490, 12890, 6780, 9001, 9002);

        ArrayList<ResourceAddress> list = new ArrayList<ResourceAddress>(ports.size());
        Map<ResourceAddress, ResourceAddress> map = new ConcurrentSkipListMap<ResourceAddress, ResourceAddress>(cmp);

        for(int port : ports) {
            ResourceAddress addr = makeLayeredResourceAddress(port);
            list.add(addr);
            map.put(addr, addr);
        }

        for(ResourceAddress expected: list) {
            ResourceAddress got = map.get(expected);
            if (expected != got) {
                throw new RuntimeException("Expected = "+expected+" got = "+got);
            }
        }
        Collections.shuffle(list);
        for(ResourceAddress expected: list) {
            ResourceAddress got = map.get(expected);
            if (expected != got) {
                throw new RuntimeException("Expected = "+expected+" got = "+got);
            }
        }
    }


    private ResourceAddress makeLayeredResourceAddress(int port) throws Exception {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("tcp.nextProtocol", "http/1.1");
        options.put("tcp.transport", URI.create("tcp://localhost:"+port));
        ResourceAddress tcp = addressFactory.newResourceAddress(new URI("tcp://localhost:8005"), options);

        ResourceAddress addr1 = addressFactory.newResourceAddress(new URI("http://localhost:8002/echo"), "ws/rfc6455");
        ResourceAddress http = addressFactory.newResourceAddress(addr1, tcp);

        ResourceAddress addr2 = addressFactory.newResourceAddress(new URI("wss://localhost:8001/echo"));
        return addressFactory.newResourceAddress(addr2, http);
    }

    private ResourceAddress makeResourceAddress(URI location) {
        return addressFactory.newResourceAddress(location);
    }

}
