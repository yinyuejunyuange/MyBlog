package org.oyyj.mycommonbase.common.commonEnum;

import lombok.Data;
import lombok.Getter;

@Getter
public enum YesOrNoEnum {
    YES(1,"1","YES"),
    NO(0,"0","NO");
    private Integer code;
    private String name;
    private String value;
    YesOrNoEnum(Integer code, String name, String value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }



}
