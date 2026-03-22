package org.oyyj.userservice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.userservice.pojo.User;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 对管理员展示的用户信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoForAdminVO {

    private String id;

    private String userName;

    private String userHead;

    private Date createTime;

    private Integer blogCount;

    private Integer comRepCount;

    private Integer toxicCount;

    private BigDecimal toxicRate;

    private Integer isUserFreeze;


    /**
     * 将 User 实体转换为 UserInfoForAdminVO
     * @param user 用户实体（不可为null）
     * @return 转换后的VO对象，若user为null则返回null
     */
    public static UserInfoForAdminVO fromUser(User user) {
        if (user == null) {
            return null;
        }
        UserInfoForAdminVO vo = new UserInfoForAdminVO();
        // id: Long -> String
        vo.setId(String.valueOf(user.getId()));
        vo.setUserName(user.getName());          // User.name → VO.userName
        vo.setUserHead(user.getImageUrl());      // User.imageUrl → VO.userHead
        vo.setCreateTime(user.getCreateTime());
        vo.setIsUserFreeze(user.getIsFreeze());  // User.isFreeze → VO.isUserFreeze
        // 其他VO特有字段（blogCount等）不设置，保持默认值null
        return vo;
    }

    /**
     * 将当前 VO 转换为 User 实体（仅填充共有字段）
     * @return User 实体对象，未映射的字段保持默认值
     */
    public  User toUser() {
        User user = new User();
        if (this.id != null) {
            user.setId(Long.valueOf(this.id));   // String → Long
        }
        user.setName(this.userName);
        user.setImageUrl(this.userHead);
        user.setCreateTime(this.createTime);
        user.setIsFreeze(this.isUserFreeze);
        // User中其他字段（如password、nickName等）不设置，保持null/默认值
        return user;
    }

}
