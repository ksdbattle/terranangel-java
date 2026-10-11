package com.terranangel.grpc;

import io.grpc.stub.StreamObserver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.grpc.server.service.GrpcService;

/**
 * gRPC 服务端：一个类实现 proto 里定义的所有方法（两个接口方法）。
 */
@GrpcService
@ConditionalOnProperty(prefix = "terranangel.grpc", name = "enabled", havingValue = "true")
public class HelloGrpcService extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String message = "Hello, " + request.getName() + "! (gRPC)";
        responseObserver.onNext(HelloResponse.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getServerStatus(EmptyRequest request, StreamObserver<StatusResponse> responseObserver) {
        responseObserver.onNext(StatusResponse.newBuilder().setStatus("RUNNING").build());
        responseObserver.onCompleted();
    }
}
