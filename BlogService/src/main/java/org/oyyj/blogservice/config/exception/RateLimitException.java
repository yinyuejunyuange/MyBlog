package org.oyyj.blogservice.config.exception;

/**
 * 流量过大 消息异常
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }

}
