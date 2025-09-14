package org.oyyj.blogservice.config.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("inventory_config")
public class ApiConfig {
    /**
     * 配置ID（主键，自增）
     */
    @TableId("id")// 自增主键
    private Long id;

    /**
     * 配置阴文名称
     */
    @TableField("name")
    private String name;

    /**
     * 配置 对应的值  没有则代表只是一个名称
     */
    @TableField("value")
    private String value;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT,value = "created_at") // 插入时自动填充
    private LocalDateTime createdAt; // 数据库字段：created_at → 驼峰命名映射

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE,value = "updated_at") // 插入/更新时自动填充
    private LocalDateTime updatedAt;


}
