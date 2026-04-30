package org.oyyj.gatewaydemo.filter;

import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.mycommonbase.utils.RedisUtil;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;


import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class TokenRateLimitFilter implements GlobalFilter, Ordered {

    private static final long SCALE_FACTOR = 1000L;
    private static final String GLOBAL_BUCKET_KEY = "gateway:rate-limit:global";
    private static final String CLIENT_BUCKET_KEY_PREFIX = "gateway:rate-limit:client:";



    @Autowired
    private RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PathPatternParser pathPatternParser = new PathPatternParser();
    private final List<PathPattern> excludePatterns = new ArrayList<>();

    @Value("${gateway-rate-limit.enabled}")
    private boolean enabled;

    @Value("${gateway-rate-limit.order}")
    private int order;

    @Value("${gateway-rate-limit.global-capacity}")
    private long globalCapacity;

    @Value("${gateway-rate-limit.global-refill-per-second}")
    private long globalRefillPerSecond;

    @Value("${gateway-rate-limit.client-capacity}")
    private long clientCapacity;

    @Value("${gateway-rate-limit.client-refill-per-second}")
    private long clientRefillPerSecond;

    @Value("${gateway-rate-limit.requested-tokens}")
    private long requestedTokens;

    @Value("${gateway-rate-limit.ttl-seconds}")
    private long ttlSeconds;

    @Value("${gateway-rate-limit.exclude-paths}")
    private String excludePaths;

    /**
     * 初始化 放行的接口路径
     */
    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(excludePaths)) {
            return;
        }
        String[] split = excludePaths.split(",");
        for (String item : split) {
            String path = item.trim();
            if (StringUtils.hasText(path)) {
                excludePatterns.add(pathPatternParser.parse(path));
            }
        }
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String requestPath = request.getPath().pathWithinApplication().value();
        if (isExcluded(requestPath)) {
            return chain.filter(exchange);
        }

        String clientKey = resolveClientKey(request);

        boolean globalAllowed = tryAcquire(
                GLOBAL_BUCKET_KEY,
                globalCapacity,
                globalRefillPerSecond,
                requestedTokens
        );

        if (!globalAllowed) {
            log.warn("网关全局限流触发 path={}", requestPath);
            return writeRateLimitResponse(exchange.getResponse(), "系统繁忙，请稍后重试");
        }

        boolean clientAllowed = tryAcquire(
                CLIENT_BUCKET_KEY_PREFIX + clientKey,
                clientCapacity,
                clientRefillPerSecond,
                requestedTokens
        );

        if (!clientAllowed) {
            log.warn("网关客户端限流触发 clientKey={}, path={}", clientKey, requestPath);
            return writeRateLimitResponse(exchange.getResponse(), "请求过于频繁，请稍后再试");
        }

        return chain.filter(exchange);
    }

    private boolean isExcluded(String requestPath) {
        if (excludePatterns.isEmpty()) {
            return false;
        }
        PathContainer pathContainer = PathContainer.parsePath(requestPath);
        for (PathPattern pattern : excludePatterns) {
            if (pattern.matches(pathContainer)) {
                return true;
            }
        }
        return false;
    }

    private String resolveClientKey(ServerHttpRequest request) {
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (StringUtils.hasText(userId)) {
            return "user:" + userId;
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (!StringUtils.hasText(realIp)) {
            realIp = request.getHeaders().getFirst("X-Forwarded-For");
            if (StringUtils.hasText(realIp) && realIp.contains(",")) {
                realIp = realIp.split(",")[0].trim();
            }
        }

        if (!StringUtils.hasText(realIp)) {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                realIp = remoteAddress.getAddress().getHostAddress();
            }
        }

        if (!StringUtils.hasText(realIp)) {
            realIp = "unknown";
        }

        return "ip:" + realIp;
    }

    @SuppressWarnings("unchecked")
    private boolean tryAcquire(String bucketKey, long capacity, long refillPerSecond, long requestTokens) {
        long capacityScaled = capacity * SCALE_FACTOR;
        long refillPerSecondScaled = refillPerSecond * SCALE_FACTOR;
        long requestedScaled = requestTokens * SCALE_FACTOR;

        List<String> keys = List.of(bucketKey + ":tokens", bucketKey + ":ts");
        long now = System.currentTimeMillis();

        try {
            List result = redisUtil.rateLimit(
                    keys,
                    String.valueOf(capacityScaled),
                    String.valueOf(refillPerSecondScaled),
                    String.valueOf(requestedScaled),
                    String.valueOf(now),
                    String.valueOf(ttlSeconds));

            if (result == null || result.isEmpty()) {
                log.warn("令牌桶脚本返回为空，bucketKey={}", bucketKey);
                return true;
            }

            Object allowObject = result.get(0);
            return Objects.equals(String.valueOf(allowObject), "1");
        } catch (Exception e) {
            log.error("令牌桶脚本执行失败，bucketKey={}", bucketKey, e);
            return true;
        }
    }

    private Mono<Void> writeRateLimitResponse(ServerHttpResponse response, String message) {
        try {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] body = objectMapper.writeValueAsBytes(ResultUtil.fail(409, message));
            DataBuffer buffer = response.bufferFactory().wrap(body);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("限流响应写入失败", e);
            String body = "{\"code\":400,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return order;
    }
}
