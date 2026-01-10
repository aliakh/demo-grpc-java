package com.example.grpc.headers;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.logging.Level;
import java.util.logging.Logger;

class HeadersClientInterceptor implements ClientInterceptor {

    private static final Logger logger = Logger.getLogger(HeadersClientInterceptor.class.getName());
    private static final Metadata.Key<String> CUSTOM_HEADER_KEY =
        Metadata.Key.of("custom_client_header", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method,
        CallOptions callOptions,
        Channel next) {
        return new SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(CUSTOM_HEADER_KEY, "custom_request_value");
                super.start(new SimpleForwardingClientCallListener<>(responseListener) {
                    @Override
                    public void onHeaders(Metadata responseHeaders) {
                        logger.log(Level.INFO, "header received from server: {0}", responseHeaders);
                        super.onHeaders(responseHeaders);
                    }
                }, headers);
            }
        };
    }
}
