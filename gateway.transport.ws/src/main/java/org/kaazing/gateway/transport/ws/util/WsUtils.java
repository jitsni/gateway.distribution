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

package org.kaazing.gateway.transport.ws.util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.util.Base64;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;

public class WsUtils {

    private static final int DIGEST_LENGTH = 16;
    public static final String SEC_WEB_SOCKET_VERSION = "Sec-WebSocket-Version";
    public static final String SEC_WEB_SOCKET_KEY = "Sec-WebSocket-Key";
    public static final String SEC_WEB_SOCKET_KEY1 = "Sec-WebSocket-Key1";
    public static final String SEC_WEB_SOCKET_KEY2 = "Sec-WebSocket-Key2";
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private WsUtils() {
        // no instances
    }
    
    /*
     * Parse a string as an integer according to the algorithm defined in the WebSocket protocol
     * (draft Hixie-76)
     * 
     * @param key Sec-WeSocket-Key[1|2] key value
     * @return
     */
    private static int parseIntKey(CharSequence key) {
        int numSpaces = 0;
        StringBuilder digits = new StringBuilder();

        // build a string from the numerical characters
        for (int i=0; i<key.length(); i++) {
            char c = key.charAt(i);
            if (' ' == c) {
                numSpaces++;
            } else if (Character.isDigit(c)) {
                digits.append(c);
            }
        }

        String s = digits.toString();
        // This may be greater than the max value for signed integers
        long n = Long.parseLong(s);
        
        // result is the numerical value divided by the number of spaces
        return (int) (n / numSpaces);
    }
    
    /*
     * Compute the MD5 sum of the three WebSocket keys
     * (draft Hixie-76)
     * 
     * @param key1    Sec-WebSocket-Key1 value
     * @param key2    Sec-WebSocket-Key2 value
     * @param key3    8 bytes immediately following WebSocket upgrade request 
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static ByteBuffer computeHash(CharSequence key1, CharSequence key2, ByteBuffer key3) throws WsDigestException, NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        ByteBuffer buf = ByteBuffer.allocate(DIGEST_LENGTH);
        
        buf.putInt(parseIntKey(key1));
        buf.putInt(parseIntKey(key2));

        // key3 must be exactly 8 bytes
        if (key3.remaining() != 8) {
            throw new WsDigestException("WebSocket key3 must be exactly 8 bytes");
        }
        
        buf.put(key3);
        
        buf.flip();
        byte[] input = new byte[DIGEST_LENGTH];
        buf.get(input, 0, DIGEST_LENGTH);
        byte[] digest = md5.digest(input);
        
        return ByteBuffer.wrap(digest);
    }

    /*
     * Compute the Sec-WebSocket-Accept key (RFC-6455)
     * 
     * @param key
     * @return
     * @throws Exception
     */
    public static String AcceptHash(String key) throws Exception {
    	String input = key + WEBSOCKET_GUID;
    	
    	MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

    	byte[] hash = sha1.digest(input.getBytes());
    	byte[] output = Base64.encodeBase64(hash);
    	return new String(output);
    }
    
    public static int calculateEncodedLengthSize(int lengthValue) {
        int ceilLog2LengthPlus1 = 32 - Integer.numberOfLeadingZeros(lengthValue);
        switch (ceilLog2LengthPlus1) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
            return 1;
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
        case 13:
        case 14:
            return 2;
        case 15:
        case 16:
        case 17:
        case 18:
        case 19:
        case 20:
        case 21:
            return 3;
        case 22:
        case 23:
        case 24:
        case 25:
        case 26:
        case 27:
        case 28:
            return 4;
        case 29:
        case 30:
        case 31:
            return 5;
        default:
            throw new IllegalArgumentException("Negative length is not supported");
        }
    }
    
    public static void encodeLength(ByteBuffer buf, int lengthValue) {
        int lengthInProgress = lengthValue;
        
        // Length-bytes are written out in order from most to
        // least significant, but are computed most efficiently (using
        // bit shifts) from least to most significant. An integer serves
        // as a temporary storage, which is then written out in reversed
        // order.
        
        int howMany = 0;
        long byteHolder = 0;
        
        do {
          byteHolder <<= 8;
          byte lv = (byte)(lengthInProgress &0x7F);
          byteHolder |= lv;
          lengthInProgress >>= 7;
          howMany++;
        } while (lengthInProgress > 0);
        
        do {
          byte bv = (byte)(byteHolder & 0xFF);
          
          byteHolder >>= 8;
          
          // The last length byte does not have the highest bit set
          if (howMany != 1) {
            bv |= (byte)0x80;
          }
          buf.put(bv);
          
        } while (--howMany > 0);
        
    }


    public static WebSocketWireProtocol guessWireProtocolVersion(HttpRequestMessage httpRequest) {
        String httpRequestVersionHeader = httpRequest.getHeader(SEC_WEB_SOCKET_VERSION);
        if ( httpRequestVersionHeader == null || httpRequestVersionHeader.length() == 0) {
            // Let's see if the request looks like Hixie 75 or 76
            if ( httpRequest.getHeader(SEC_WEB_SOCKET_KEY1) != null &&
                 httpRequest.getHeader(SEC_WEB_SOCKET_KEY2) != null ) {
                return WebSocketWireProtocol.HIXIE_76;
            } else {
                return WebSocketWireProtocol.HIXIE_75;
            }
        } else {
            try {
                return WebSocketWireProtocol.valueOf(Integer.parseInt(httpRequestVersionHeader));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }


    public static String negotiateWebSocketProtocol(HttpAcceptSession session, String protocolHeaderName,
                                                    List<String> clientRequestedWsProtocols,
                                                    List<String> serverWsProtocols) throws WsHandshakeNegotiationException {

        if (clientRequestedWsProtocols != null) {
            List<String> wsCandidateProtocols = new ArrayList<String>(serverWsProtocols);
            wsCandidateProtocols.retainAll(clientRequestedWsProtocols);
            if (wsCandidateProtocols.isEmpty()) {
                if (!serverWsProtocols.contains(null)) {
                    session.setStatus(HttpStatus.CLIENT_NOT_FOUND);
                    session.setReason("WebSocket SubProtocol Not Found");
                    session.close(false);
                    throw new WsHandshakeNegotiationException("WebSocket SubProtocol Not Found");
                }
            } else {
                final String chosenProtocol = wsCandidateProtocols.get(0);
                session.addWriteHeader(protocolHeaderName, chosenProtocol);
                return chosenProtocol;
            }
        }
        return null;
    }

}

