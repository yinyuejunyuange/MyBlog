package org.oyyj.blogservice.common.BlogEnum;

import lombok.Getter;

@Getter
public enum PublishEnum {
    PUBLISH("publish"),
    SAVE("save"),
    TIMED("timed");
    private final String value;
    PublishEnum(String value) {
        this.value = value;
    }


}
