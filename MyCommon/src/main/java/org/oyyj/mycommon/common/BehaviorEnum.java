package org.oyyj.mycommon.common;

import lombok.Getter;

@Getter
public enum BehaviorEnum {

    VIEW(0,0.3,"有效阅读"),
    LIKE(1,0.6,"点赞"),
    COMMENT(2,0.4,"评论"),
    COLLECT(3,0.7,"收藏"),
    SHARE(4,0.5,"分享");

    private final Integer code;
    private final Double weight;
    private final String name;
    BehaviorEnum(Integer code, Double weight, String name) {
        this.code = code;
        this.weight = weight;
        this.name = name;
    }

    public static BehaviorEnum getBehaviorEnum(Integer code){
        for (BehaviorEnum behaviorEnum : BehaviorEnum.values()) {
            if (behaviorEnum.getCode().equals(code)) {
                return behaviorEnum;
            }
        }
        return null;
    }

    /**
     * 传入的参数是否合法
     * @param code
     * @return
     */
    public static boolean isStatusLaw(Integer code){
        if(code == null){
            return false;
        }
        for (BehaviorEnum behaviorEnum : BehaviorEnum.values()) {
            if (behaviorEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

}
