package com.terranangel.grpc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * gRPC 客户端：把 stub 注册成 Spring Bean，业务代码注入即可调用。
 */
@Configuration
@ConditionalOnProperty(prefix = "terranangel.grpc", name = "enabled", havingValue = "true")
public class GrpcHelloClient {

    @Bean
    public HelloServiceGrpc.HelloServiceBlockingStub helloServiceStub(GrpcChannelFactory channelFactory) {
        return HelloServiceGrpc.newBlockingStub(channelFactory.createChannel("hello"));
    }
}
