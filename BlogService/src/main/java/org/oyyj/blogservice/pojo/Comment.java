package org.oyyj.blogservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Comment {
    private Long id; // 评论id
    private Long userId;
    private Long blogId; // 关联的博客id
    private String content;
    private List<Reply> replytList;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
    private Integer status; // 当前评论状态
}
