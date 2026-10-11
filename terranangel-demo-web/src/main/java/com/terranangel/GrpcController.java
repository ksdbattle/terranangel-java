package com.terranangel;

import com.terranangel.grpc.HelloRequest;
import com.terranangel.grpc.HelloResponse;
import com.terranangel.grpc.HelloServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * web -> gRPC 调用入口：浏览器访问 HTTP 接口，内部通过 gRPC 调服务。
 */
@RestController
@ConditionalOnProperty(prefix = "terranangel.grpc", name = "enabled", havingValue = "true")
public class GrpcController {

    @Autowired
    private HelloServiceGrpc.HelloServiceBlockingStub helloServiceStub;

    @GetMapping("/callGrpc")
    public String callGrpc(@RequestParam(defaultValue = "world") String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloResponse response = helloServiceStub.sayHello(request);
        return "web 调用 gRPC 成功: " + response.getMessage();
    }
}
