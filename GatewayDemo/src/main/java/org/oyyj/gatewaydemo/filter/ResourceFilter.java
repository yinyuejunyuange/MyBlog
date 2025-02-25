package org.oyyj.gatewaydemo.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.util.Collections;

@Configuration
public class ResourceFilter implements GlobalFilter , Ordered {

    private final static String FROMWORD="FROM_GATEWAY";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("from",FROMWORD).build();
        /*
        exchange.getRequest(): 获取当前的 HTTP 请求。
mutate(): 创建一个可变的请求构建器，使你可以修改请求的属性。
.header("from", "gateway"): 向请求添加一个自定义 HTTP 头，名为 from，值为 gateway。这可以用于标识请求的来源。
build(): 构建并返回修改后的请求对象。
        * */

        exchange= exchange.mutate().request(request).build();

        return getVoidMono(exchange,chain);
    }

    // 配置跨域否则不生效果
    private Mono<Void> getVoidMono(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.defer(() -> {
            exchange.getResponse().getHeaders().entrySet().stream()
                    .filter(kv -> kv.getValue() != null && kv.getValue().size() > 1)
                    .filter(kv -> kv.getKey().equals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
                            || kv.getKey().equals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                    .forEach(kv -> kv.setValue(Collections.singletonList(kv.getValue().get(0))));
            return chain.filter(exchange);
        }));
    }


    @Override
    public int getOrder() {
        return 0;  // 确保再跳转之前实现
    }
}
