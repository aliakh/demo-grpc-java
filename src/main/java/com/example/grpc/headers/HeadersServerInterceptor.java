package com.example.grpc.headers;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

class HeadersServerInterceptor implements ServerInterceptor {

    private static final Logger logger = Logger.getLogger(HeadersServerInterceptor.class.getName());
    private static final Metadata.Key<String> CUSTOM_HEADER_KEY =
        Metadata.Key.of("custom_server_header", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        Metadata requestHeaders,
        ServerCallHandler<ReqT, RespT> next) {
        logger.log(Level.INFO, "header received from client: {0}", requestHeaders);
        return next.startCall(new SimpleForwardingServerCall<>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                responseHeaders.put(CUSTOM_HEADER_KEY, "custom_response_value");
                super.sendHeaders(responseHeaders);
            }
        }, requestHeaders);
    }
}
