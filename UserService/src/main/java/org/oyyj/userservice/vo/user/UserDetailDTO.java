package org.oyyj.userservice.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.mycommon.pojo.dto.blog.Blog12MonthDTO;
import org.oyyj.mycommon.pojo.vo.UserComRepVO;
import org.oyyj.userservice.pojo.User;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 用户详情
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailDTO {

    private String id;

    private String userName;

    private String userHead;

    private String introduction;

    private Date createTime;

    private Integer blogCount;

    private Integer comRepCount;

    private Integer toxicCount;

    private BigDecimal toxicRate;

    private Integer isUserFreeze;

    private Blog12MonthDTO  blog12MonthDTO; // 用户12月博客创作信息

    private List<UserComRepVO> userComRepVOList; // 用户恶评



    public static UserDetailDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        UserDetailDTO vo = new UserDetailDTO();
        // id: Long -> String
        vo.setId(String.valueOf(user.getId()));
        vo.setUserName(user.getName());          // User.name → VO.userName
        vo.setUserHead(user.getImageUrl());      // User.imageUrl → VO.userHead
        vo.setCreateTime(user.getCreateTime());
        vo.setIsUserFreeze(user.getIsFreeze());  // User.isFreeze → VO.isUserFreeze
        vo.setIntroduction(user.getIntroduce());

        return vo;
    }

}
