package org.oyyj.adminservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentAdminVO {

    private String id;
    private String blogName;
    private String userName;
    private String context;
    private Date createTime;
    private Date updateTime;
    private Integer isVisible;

}
