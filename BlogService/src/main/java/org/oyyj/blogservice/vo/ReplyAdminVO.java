package org.oyyj.blogservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReplyAdminVO {
    private String id;
    private String userName;
    private String blogName; // 来自与那个blog
    private String comment; // 回复的评论内容
    private String context; // 回复的内容
    private Date createTime;
    private Date updateTime;
    private Integer isVisible;

}
