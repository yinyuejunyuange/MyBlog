package org.oyyj.blogservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Reply {
    private Long id; //回复id
    private Long userId; // 回复者id
    private Long commentId;// 关联的评论id
    private String content; // 回复内容
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;

}
