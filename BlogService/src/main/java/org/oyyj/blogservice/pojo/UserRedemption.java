package org.oyyj.blogservice.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户领取记录表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRedemption {

    @TableId("id")
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("inventory_id")
    private Long inventoryId;
    @TableField("redeemed_at")
    private LocalDateTime redeemTime;

}
