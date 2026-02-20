package org.oyyj.blogservice.common.BlogEnum;

import lombok.Getter;

@Getter
public enum TimedModeEnum {

    SCHEDULE("schedule"),
    DELAY("delay");

    private final String value;
    TimedModeEnum(String value) {
        this.value = value;
    }

}
