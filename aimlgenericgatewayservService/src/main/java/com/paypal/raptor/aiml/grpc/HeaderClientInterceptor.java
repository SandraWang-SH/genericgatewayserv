package com.paypal.raptor.aiml.grpc;

import com.paypal.raptor.aiml.service.impl.PredictServiceImpl;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.slf4j.LoggerFactory;

/**
 * A interceptor to handle client header.
 */
public class HeaderClientInterceptor implements ClientInterceptor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PredictServiceImpl.class);

    static final Metadata.Key<String> NAMESAPCE_KEY =
            Metadata.Key.of("namespace", Metadata.ASCII_STRING_MARSHALLER);

    static final Metadata.Key<String> SELDON_KEY =
            Metadata.Key.of("seldon", Metadata.ASCII_STRING_MARSHALLER);

    private String NAMESAPCE_VALUE;
    private String SELDON_VALUE;

    public HeaderClientInterceptor(String namespace_val, String seldon_val) {
        NAMESAPCE_VALUE = namespace_val;
        SELDON_VALUE = seldon_val;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                /* put custom header */
                headers.put(NAMESAPCE_KEY, NAMESAPCE_VALUE);
                headers.put(SELDON_KEY, SELDON_VALUE);

                super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        /**
                         * if you don't need to receive header from server,
                         * you can use {@link io.grpc.stub.MetadataUtils#attachHeaders}
                         * directly to send header
                         */
                        logger.info("header received from server:" + headers);
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}