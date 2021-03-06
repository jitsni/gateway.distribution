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

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.kaazing.gateway.transport.wseb.WsebEncodingStrategy.TEXT_AS_BINARY;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsProtocol;
import org.kaazing.gateway.transport.ws.extension.ActiveWsExtensions;
import org.kaazing.gateway.transport.wseb.filter.EncodingFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebEncodingCodecFilter.EscapeTypes;
import org.kaazing.gateway.transport.wseb.filter.WsebReconnectFilter;
import org.kaazing.gateway.transport.wseb.filter.WsebTextAsBinaryEncodingCodecFilter;
import org.kaazing.gateway.util.Encoding;
import org.kaazing.mina.netty.IoSessionIdleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsebDownstreamHandler extends IoHandlerAdapter<HttpAcceptSession> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WsebDownstreamHandler.class);

    static final AttributeKey TIMEOUT_FUTURE_KEY = new AttributeKey(WsebDownstreamHandler.class, "timeoutFuture");

    private static final String CODEC_FILTER = WsebProtocol.NAME + "#codec";
    private static final String ENCODING_FILTER = WsebProtocol.NAME + "#escape";
    private static final String LOGGER_NAME = String.format("transport.%s.accept", WsebProtocol.NAME);
    private static final String RECONNECT_FILTER = WsebProtocol.NAME + "#reconnect";

    private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    // TODO: make this setting available via configuration, with a reasonable default
    static final long TIME_TO_TIMEOUT_RECONNECT_MILLIS = TimeUnit.SECONDS.toMillis(60L);

    private final String contentType;
    private final WsebSession wsebSession;
    private final WsebEncodingCodecFilter codec;
    private final IoFilter encoding;
    private final ScheduledExecutorService scheduler;
    private IoSessionIdleTracker inactivityTracker = null;
    private final BridgeServiceFactory bridgeServiceFactory;
 
    public WsebDownstreamHandler(ResourceAddress nextProtocolAddress,  WsebSession wsebSession, ScheduledExecutorService scheduler,
                                 WsebEncodingStrategy encodingStrategy, IoSessionIdleTracker inactivityTracker, BridgeServiceFactory bridgeServiceFactory) {
        this(nextProtocolAddress, wsebSession, scheduler, "application/octet-stream", encodingStrategy, inactivityTracker, bridgeServiceFactory);
    }

    public WsebDownstreamHandler(ResourceAddress nextProtocolAddress, WsebSession wsebSession, ScheduledExecutorService scheduler,
                                 String contentType, WsebEncodingStrategy encodingStrategy, IoSessionIdleTracker inactivityTracker, BridgeServiceFactory bridgeServiceFactory) {
        this(nextProtocolAddress, wsebSession, scheduler, contentType, null, encodingStrategy, inactivityTracker, bridgeServiceFactory);
    }

    public WsebDownstreamHandler(ResourceAddress nextProtocolAddress, WsebSession wsebSession, ScheduledExecutorService scheduler, String contentType,
                                 Encoding escapeEncoding, WsebEncodingStrategy encodingStrategy, IoSessionIdleTracker inactivityTracker, BridgeServiceFactory bridgeServiceFactory) {
        this.wsebSession = wsebSession;
        this.contentType = contentType;
        if (encodingStrategy == TEXT_AS_BINARY) {
            // 3.5 clients
            this.codec = new WsebTextAsBinaryEncodingCodecFilter();
            this.encoding = (escapeEncoding != null) ? new EncodingFilter(escapeEncoding) : null;
        }
        else {
            // 4.0 clients - escape is inside codecFilter
            this.codec = escapeEncoding != null ? new WsebEncodingCodecFilter(EscapeTypes.ESCAPE_ZERO_AND_NEWLINES) : new WsebEncodingCodecFilter();
            this.encoding = null;
        }
        this.scheduler = scheduler;
        this.inactivityTracker = inactivityTracker;
        this.bridgeServiceFactory = bridgeServiceFactory;
    }

    @Override
    protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WsebDownstreamHandler.doExceptionCaught", cause);
        }

        WsebSession wseSession = getSession(session);
        if (wseSession != null && !wseSession.isClosing()) {
            wseSession.reset(cause);
            //cause.printStackTrace(System.err);
        }
        else {
            if (logger.isDebugEnabled()) {
                String message = format("Exception while handling HTTP downstream for WsebSession: %s", cause);
                if (logger.isTraceEnabled()) {
                    // note: still debug level, but with extra detail about the exception
                    logger.debug(message, cause);
                }
                else {
                    logger.debug(message);
                }
            }
            session.close(true);
        }
    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {
        WsebSession wsebSession = getSession(session);

        // WseSession may have been closed asynchronously
        // and possibly also removed from the session map
        if (wsebSession == null || wsebSession.isClosing()) {
            session.close(false);
            return;
        }

        IoFilterChain bridgeFilterChain = session.getFilterChain();
        addBridgeFilters(bridgeFilterChain, wsebSession); // pass in wseb session in case bridge filters need access to ws session data (e.g. extensions)

        // check if this session requests a short or a long keepalive timeout
        int clientIdleTimeout = ("20".equals(session.getParameter(".kkt"))) ? 20 : wsebSession.getClientIdleTimeout();

        // we don't need to send idletimeout keep-alive messages if we're already sending PINGs for inactivity timeout
        // at high enough frequency 
        int inactivityTimeoutInSeconds = (int) (wsebSession.getInactivityTimeout() / 1000); 
        if (inactivityTimeoutInSeconds == 0 || inactivityTimeoutInSeconds > clientIdleTimeout) {
            session.getConfig().setWriterIdleTime(clientIdleTimeout / 2);
        }
        
        // most clients use GET for downstream (empty POST okay too)
        // defer attach writer to message received for non-empty POSTs (such as Flash client)
        String contentLength = session.getReadHeader(HttpHeaders.HEADER_CONTENT_LENGTH);
        if (contentLength == null || parseInt(contentLength) == session.getReadBytes()) {
            reconnectSession(session, wsebSession);
        }

        if (inactivityTracker != null) {
            // Activate inactivity timeout only when downstream has connected (timeout between create and downstream is handled separately)
            inactivityTracker.addSession(wsebSession);
        }
    }

    @Override
    protected void doMessageReceived(HttpAcceptSession session, Object message)
            throws Exception {
        String contentLength = session.getReadHeader(HttpHeaders.HEADER_CONTENT_LENGTH);
        if (contentLength != null && parseInt(contentLength) == session.getReadBytes()) {
            reconnectSession(session, wsebSession);
        }
    }

    @Override
    protected void doSessionIdle(HttpAcceptSession session, IdleStatus status) throws Exception {
        if (status == IdleStatus.WRITER_IDLE) {
            session.write(WsCommandMessage.NOOP);
        }
        super.doSessionIdle(session, status);
    }

    public void addBridgeFilters(IoFilterChain bridgeFilterChain, WsebSession wsebSession) {
        bridgeFilterChain.addLast(CODEC_FILTER, codec);

        if (encoding != null) {
            bridgeFilterChain.addBefore(CODEC_FILTER, ENCODING_FILTER, encoding);
        }

        final IoSession session = bridgeFilterChain.getSession();
        final ActiveWsExtensions extensions = ActiveWsExtensions.get(wsebSession);

        codec.setExtensions(session, extensions);
    }

    public void removeBridgeFilters(IoFilterChain filterChain) {
        removeFilter(filterChain, CODEC_FILTER);
        removeFilter(filterChain, ENCODING_FILTER);
    }

    private WsebSession getSession(HttpAcceptSession session) throws Exception {
        boolean traceEnabled = logger.isTraceEnabled();

        if (traceEnabled) {
            logger.trace("Remote address resource = '"+session.getRemoteAddress().getResource()+"'");
        }

        return wsebSession;
    }

    private void reconnectSession(final HttpAcceptSession session, final WsebSession wsebSession) throws Exception {
        // cancel the timeout future if it is found
        ScheduledFuture<?> timeoutFuture = (ScheduledFuture<?>)wsebSession.removeAttribute(TIMEOUT_FUTURE_KEY);
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }

        // KG-10590 (Nascar) For backwards compatibility with Flash client from HTML5 release 3.5.1.19 when running on IE 11,
        // we pretend downstream url parameter contains ".kp=2048&.kcc=private&.kf=200"
        String userAgent = session.getReadHeader("User-Agent");
        boolean isClientIE11 = false;
        if (userAgent != null && userAgent.contains("Trident/7.0")) {
        	isClientIE11 = true;
        }
        // check for polling strategy
        boolean longPoll = false;

        // check to see if this session can stream, otherwise force long polling
        if (!HttpUtils.canStream(session)) {
            // Note: Silverlight does not support redirecting from http to https
            //       so check .kd=s parameter (downstream=same) to prevent redirecting to https
            if (!"s".equals(session.getParameter(".kd"))) {
                // lookup secure acceptURI
                URI secureAcceptURI = locateSecureAcceptURI(session);
                if (secureAcceptURI != null) {
                    URI pathInfo = session.getPathInfo();
                    String secureAuthority = secureAcceptURI.getAuthority();
                    String secureAcceptPath = secureAcceptURI.getPath();
                    URI request = URI.create("https://" + secureAuthority + secureAcceptPath + pathInfo.toString());

                    // send redirect response
                    session.setStatus(HttpStatus.REDIRECT_MOVED_PERMANENTLY);
                    session.setWriteHeader("Location", request.toString());
                    session.close(false);

                    // we have sent the redirect and closed the session so end reconnect here
                    return;
                }
            }

            longPoll = true;
        }

        if (!longPoll) {
            // In long-polling, Content-Length would be sent. So, don't send Connection:close
            // disable pipelining, avoid the need for HTTP chunking
            session.setWriteHeader("Connection", "close");
        }

        // set the content type header
        String contentType = this.contentType;

        // look for content type override
        String contentTypeOverride = session.getParameter(".kc");
        if (contentTypeOverride != null) {
            if (contentTypeOverride.indexOf(';') == -1) {
                contentTypeOverride += ";charset=UTF-8";
            }
            contentType = contentTypeOverride;
        }

        session.setWriteHeader("X-Content-Type-Options", "nosniff");
        session.setWriteHeader("Content-Type", contentType);
        session.setWriteHeader("X-Idle-Timeout", String.valueOf(wsebSession.getClientIdleTimeout()));

        // look for mime detection padding override
        if(session.getParameter(".kns") != null) {
            // this prevents IE7 from interpreting WSEB as application/octet-stream
            // the exact content and minimum length needed are not yet known
            session.setWriteHeader("X-Content-Type-Nosniff", "abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz1234");
        }

        // configure cache control strategy
        String cacheControl = "no-cache";

        String cacheControlOverride = session.getParameter(".kcc");
        if (isClientIE11 && cacheControlOverride == null) {
        	cacheControlOverride = "private"; //KG-10590 add .kcc=private for IE11 client
        }
        if (cacheControlOverride != null) {
            cacheControl = cacheControlOverride;
        }

        session.setWriteHeader("Cache-Control", cacheControl);

        if (longPoll) {
            session.setAttribute(WsebAcceptor.CLIENT_BUFFER_KEY, 0L);
        }
        else {
            // check for client buffer setting
            String clientBuffer = session.getParameter(".kb");
            if (clientBuffer != null) {
                // Note: specifying .kb=X on the URL at the client
                //       overrides the default client buffer size
                long bufferSize = Long.parseLong(clientBuffer) * 1024L;
                session.setAttribute(WsebAcceptor.CLIENT_BUFFER_KEY, bufferSize);
            }
        }

        // check to see if we need to add a padding message to the end of the sent messages
        String clientPadding = session.getParameter(".kp");
        if (isClientIE11 && clientPadding == null) {
        	clientPadding = "2048"; //KG-10590 add .kp=2048 for IE11 client
        }
        if (clientPadding != null) {
            int paddingSize = Integer.parseInt(clientPadding);
            session.setAttribute(WsebAcceptor.CLIENT_PADDING_KEY, paddingSize);
            session.setAttribute(WsebAcceptor.BYTES_WRITTEN_ON_LAST_FLUSH_KEY, new Long(0));

            if (paddingSize == 0) {
                session.setWriteHeader("X-Content-Type-Options", "nosniff");
            }
        }

        // check to see if we need to add block padding to the end of the sent messages
        String clientBlockPadding = session.getParameter(".kbp");
        if (clientBlockPadding != null) {
            int paddingSize = Integer.parseInt(clientBlockPadding);
            session.setAttribute(WsebAcceptor.CLIENT_BLOCK_PADDING_KEY, new Integer(paddingSize));
            session.setWriteHeader("Content-Encoding", "gzip");
        }

        // WseSession may have been closed asynchronously
        // and possibly also removed from the session map
        if (wsebSession == null || wsebSession.isClosing()) {
            session.close(false);
            return;
        }

        session.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture future) {
                // schedule timeout monitor for this wsfSession
                if (!wsebSession.isClosing()) {
                    // Note: proactively detach the writer to support long-polling
                    //       however, when session is closing, make sure to leave
                    //       writer intact so that parent TCP or SSL session can be closed
                    if (wsebSession.detachWriter(session)) {
                        ScheduledFuture<?> timeoutFuture = scheduler.schedule(wsebSession.getTimeoutCommand(), TIME_TO_TIMEOUT_RECONNECT_MILLIS, TimeUnit.MILLISECONDS);
                        wsebSession.setAttribute(TIMEOUT_FUTURE_KEY, timeoutFuture);
                    }
                }
            }
        });

        final CloseFuture wsebCloseFuture = wsebSession.getCloseFuture();
        final IoFutureListener<CloseFuture> listener = new IoFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture future) {
                // clean up scheduler reference to emulated session to avoid memory leak
                ScheduledFuture<?> timeoutFuture = (ScheduledFuture<?>)wsebSession.removeAttribute(TIMEOUT_FUTURE_KEY);
                if (timeoutFuture != null && !timeoutFuture.isDone()) {
                    timeoutFuture.cancel(false);
                }

                // Note: this pro-active removal of the session reference from the timeout task
                //       prevents build up of session objects until the scheduler performs
                //       cleanup of canceled tasks, otherwise a temporary memory leak can occur
                wsebSession.clearTimeoutCommand();

                // Note: a reference to the HTTP downstream session is pinned by this listener
                //       and must be removed to avoid a memory leak (see below)
                session.close(false);
            }
        };
        // detect when emulated session is closed to force downstream to close
        wsebCloseFuture.addListener(listener);
        // detect when downstream is closed to remove downstream reference from emulated session
        session.getCloseFuture().addListener(new IoFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture future) {
                // Note: a reference to the HTTP downstream session was pinned by the listener
                //       and must be removed to avoid a memory leak (see above)
                wsebCloseFuture.removeListener(listener);
            }
        });

        // attach now or attach after commit if header flush is required
        if (!longPoll) {
            // currently this is required for Silverlight as it seems to want some data to be
            // received before it will start to deliver messages
            // this is also needed to detect that streaming has initialized properly
            // so we don't fall back to encrypted streaming or long polling
            session.write(WsCommandMessage.NOOP);

            String flushDelay = session.getParameter(".kf");
            if (isClientIE11 && flushDelay == null) {
            	flushDelay = "200";   //KG-10590 add .kf=200 for IE11 client
            }
            if (flushDelay != null) {
            	final long flushDelayMillis = Integer.parseInt(flushDelay);
                // commit session and write out headers and any messages already in the queue
                CommitFuture commitFuture = session.commit();
                commitFuture.addListener(new IoFutureListener<CommitFuture>() {
                    @Override
                    public void operationComplete(CommitFuture future) {
                        // attach http session to wsf session
                        // after delay to force Silverlight client to notice payload
                        if (flushDelayMillis > 0L) {
                            Runnable command = new AttachParentCommand(wsebSession, session, flushDelayMillis);
                            scheduler.schedule(command, flushDelayMillis, TimeUnit.MILLISECONDS);
                        }
                        else {
                            wsebSession.attachWriter(session);
                        }
                    }
                });
            }
            else {
                // attach http session to wse session
                wsebSession.attachWriter(session);
            }
        }
        else {
            // attach http session to wse session
            wsebSession.attachWriter(session);
        }
    }

    private URI locateSecureAcceptURI(HttpAcceptSession session) throws Exception {
        // TODO: same-origin requests must consider cross-origin access control
        //       internal redirect to secure resource should not trigger 403 Forbidden
        ResourceAddress localAddress = (ResourceAddress)session.getLocalAddress();
        URI resource = localAddress.getResource();
        Protocol resourceProtocol = bridgeServiceFactory.getTransportFactory().getProtocol(resource);
        if (WsebProtocol.WSEB_SSL == resourceProtocol || WsProtocol.WSS == resourceProtocol) {
            return resource;
        }
        return null;
    }

    private class AttachParentCommand implements Runnable {

        private final WsebSession wsebSession;
        private final BridgeSession parent;
        private final long flushDelayMillis;

        private AttachParentCommand(WsebSession wsebSession, BridgeSession parent, long flushDelayMillis) {
            this.wsebSession = wsebSession;
            this.parent = parent;
            this.flushDelayMillis = flushDelayMillis;
        }

        @Override
        public void run() {
            wsebSession.attachWriter(parent);

            // attaching the parent flushes buffered writes to HTTP response
            // but if connection has high latency, then intermediate TCP node
            // can cause server-delayed write to be combined into the same TCP packet
            // defeating the purpose of the delay (needed by Silverlight)
            // therefore, write a comment frame a little later as a backup to make
            // sure that the connection does not get stalled

            scheduler.schedule(new FlushCommand(wsebSession), flushDelayMillis * 2, TimeUnit.MILLISECONDS);
            scheduler.schedule(new FlushCommand(wsebSession), flushDelayMillis * 4, TimeUnit.MILLISECONDS);
            scheduler.schedule(new FlushCommand(wsebSession), flushDelayMillis * 8, TimeUnit.MILLISECONDS);
        }
    }

    private class FlushCommand implements Runnable {

        private final WsebSession session;

        public FlushCommand(WsebSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            IoSession parent = session.getParent();
            if (parent != null && !parent.isClosing()) {
                parent.write(WsCommandMessage.NOOP);
            }
        }

    }

    protected final void removeFilter(IoFilterChain filterChain, String name) {
        if (filterChain.contains(name)) {
            filterChain.remove(name);
        }
    }

    protected final void removeFilter(IoFilterChain filterChain, IoFilter filter) {
        if (filterChain.contains(filter)) {
            filterChain.remove(filter);
        }
    }

}
