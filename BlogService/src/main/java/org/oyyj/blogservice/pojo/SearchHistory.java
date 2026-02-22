package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("search_history")
public class SearchHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("query_raw")
    private String queryRaw;
    @TableField("query_norm")
    private String queryNorm;
    @TableField("is_visible")
    private Integer isVisible;
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
    @TableField("created_at")
    private Date createdAt;
}
