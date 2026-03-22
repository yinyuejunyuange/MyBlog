package org.oyyj.mycommon.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserComRepVO {

    private String id;

    private String comment;

    /**
     * 类型 评论 回复
     */
    private String type;

    private String blogId;

    private String blogName;

    private Integer isVisible;

    /**
     * 攻击的种类 ','逗号间隔
     */
    private String mulType;

    /**
     * 是否具有攻击性 0：无 1：冒犯性 2：攻击性
     */
    private Integer isToxic;
}
