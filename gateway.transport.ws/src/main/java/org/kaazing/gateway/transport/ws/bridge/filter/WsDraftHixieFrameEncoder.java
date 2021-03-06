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

package org.kaazing.gateway.transport.ws.bridge.filter;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;

import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsDraftHixieFrameEncoder extends AbstractWsFrameEncoder {

    public WsDraftHixieFrameEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }
    
    public WsDraftHixieFrameEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        super(cachingEncoder, allocator);
    }

    protected IoBufferEx doTextEscapedEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, byte[] escapedBytes) {
        return WsDraftHixieFrameEncodingSupport.doTextEscapedEncode(allocator, flags, message, escapedBytes);
    }

    @Override
    protected IoBufferEx doContinuationEscapedEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, byte[] escapedBytes) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    protected IoBufferEx doBinaryEscapedEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, byte[] escapedBytes) {
        return WsDraftHixieFrameEncodingSupport.doBinaryEscapedEncode(allocator, flags, message, escapedBytes);
    }

    protected IoBufferEx doTextEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return WsDraftHixieFrameEncodingSupport.doTextEncode(allocator, flags, message);
    }

    @Override
    protected IoBufferEx doContinuationEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    protected IoBufferEx doBinaryEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return WsDraftHixieFrameEncodingSupport.doBinaryEncode(allocator, flags, message);
    }

    protected IoBufferEx doCloseEncode(IoBufferAllocatorEx<?> allocator, int flags, WsCloseMessage message) {
        return WsDraftHixieFrameEncodingSupport.doCloseEncode(allocator, flags);
    }

}
