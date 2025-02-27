package org.oyyj.blogservice.enums;

import java.util.Map;

public enum TypeEnum {
    AFTER_END,//后端
    BEFORE_END,

    JAVA,
    C11,
    C,
    PYTHON,
    GOLANG,

    VUE,
    HTML,
    CSS,
    JAVASCRIPT,

    COMPUTER,
    OS;

    public String getType(TypeEnum type){
        return String.valueOf(type);
    }
}
