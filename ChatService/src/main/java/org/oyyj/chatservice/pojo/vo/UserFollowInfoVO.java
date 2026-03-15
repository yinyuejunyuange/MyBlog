package org.oyyj.chatservice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.chatservice.pojo.UserFollowInfo;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowInfoVO {

    private String id;

    private String userId;

    private Integer isUserKnow;

    private String followUserId;

    private Integer isDelete;

    private Date createdAt;

    private Date updatedAt;

    private String userName;

    private String userHead;


    /**
     * Entity -> VO
     */
    public static UserFollowInfoVO fromEntity(UserFollowInfo entity){
        if(entity == null){
            return null;
        }

        UserFollowInfoVO vo = new UserFollowInfoVO();

        vo.setId(entity.getId() == null ? null : entity.getId().toString());
        vo.setUserId(entity.getUserId() == null ? null : entity.getUserId().toString());
        vo.setFollowUserId(entity.getFollowUserId() == null ? null : entity.getFollowUserId().toString());

        vo.setIsUserKnow(entity.getIsUserKnow());
        vo.setIsDelete(entity.getIsDelete());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setUserName(entity.getUserName());
        vo.setUserHead(entity.getUserHead());

        return vo;
    }


    /**
     * VO -> Entity
     */
    public static UserFollowInfo toEntity(UserFollowInfoVO vo){
        if(vo == null){
            return null;
        }

        UserFollowInfo entity = new UserFollowInfo();

        entity.setId(vo.getId() == null ? null : Long.parseLong(vo.getId()));
        entity.setUserId(vo.getUserId() == null ? null : Long.parseLong(vo.getUserId()));
        entity.setFollowUserId(vo.getFollowUserId() == null ? null : Long.parseLong(vo.getFollowUserId()));

        entity.setIsUserKnow(vo.getIsUserKnow());
        entity.setIsDelete(vo.getIsDelete());
        entity.setCreatedAt(vo.getCreatedAt());
        entity.setUpdatedAt(vo.getUpdatedAt());
        entity.setUserName(vo.getUserName());
        entity.setUserHead(vo.getUserHead());

        return entity;
    }


    /**
     * List<Entity> -> List<VO>
     */
    public static List<UserFollowInfoVO> fromEntityList(List<UserFollowInfo> list){
        if(list == null){
            return null;
        }

        return list.stream()
                .map(UserFollowInfoVO::fromEntity)
                .collect(Collectors.toList());
    }

}