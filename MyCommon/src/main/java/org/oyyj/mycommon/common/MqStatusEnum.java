package org.oyyj.mycommon.common;

import lombok.Getter;

@Getter
public enum MqStatusEnum {
    FAIL_SEND(-2,"发送失败"),
    NOT_SEND(-1,"未发送"),
    PENDING(0,"待处理"),
    PROCESSING(2,"处理中"),
    SUCCESS(1,"处理成功"),
    FAIL(3,"失败/进入死信队列"),
    MANUAL_PROCESS(4,"人工处理");

    private final Integer code;
    private final String message;

    MqStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
