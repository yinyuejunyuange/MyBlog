package org.oyyj.chatservice.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oyyj.chatservice.pojo.UserLikeStartInfo;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLikeStartInfoVO {

    private String id;

    /**
     * 操作者
     */
    private String userId;

    /**
     * 被行为者
     */
    private String behaviorId;

    private String targetId;

    /**
     * 点赞类型 1文章 2评论 3回复
     */
    private Integer targetType;

    /**
     * 行为 1点赞 2收藏
     */
    private Integer behaviour;

    private Date createdAt;

    private Date updatedAt;

    private String behaviorUserName;

    private String behaviorUserHead;

    private String targetContent;


    /**
     * Entity -> VO
     */
    public static UserLikeStartInfoVO fromEntity(UserLikeStartInfo entity) {

        if (entity == null) {
            return null;
        }

        UserLikeStartInfoVO vo = new UserLikeStartInfoVO();

        vo.setId(entity.getId() == null ? null : entity.getId().toString());
        vo.setUserId(entity.getUserId() == null ? null : entity.getUserId().toString());
        vo.setBehaviorId(entity.getBehaviorId() == null ? null : entity.getBehaviorId().toString());
        vo.setTargetId(entity.getTargetId() == null ? null : entity.getTargetId().toString());

        vo.setTargetType(entity.getTargetType());
        vo.setBehaviour(entity.getBehaviour());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        vo.setBehaviorUserName(entity.getBehaviorUserName());
        vo.setBehaviorUserHead(entity.getBehaviorUserHead());
        vo.setTargetContent(entity.getTargetContent());

        return vo;
    }


    /**
     * VO -> Entity
     */
    public static UserLikeStartInfo toEntity(UserLikeStartInfoVO vo) {

        if (vo == null) {
            return null;
        }

        UserLikeStartInfo entity = new UserLikeStartInfo();

        entity.setId(vo.getId() == null ? null : Long.parseLong(vo.getId()));
        entity.setUserId(vo.getUserId() == null ? null : Long.parseLong(vo.getUserId()));
        entity.setBehaviorId(vo.getBehaviorId() == null ? null : Long.parseLong(vo.getBehaviorId()));
        entity.setTargetId(vo.getTargetId() == null ? null : Long.parseLong(vo.getTargetId()));

        entity.setTargetType(vo.getTargetType());
        entity.setBehaviour(vo.getBehaviour());
        entity.setCreatedAt(vo.getCreatedAt());
        entity.setUpdatedAt(vo.getUpdatedAt());

        entity.setBehaviorUserName(vo.getBehaviorUserName());
        entity.setBehaviorUserHead(vo.getBehaviorUserHead());
        entity.setTargetContent(vo.getTargetContent());

        return entity;
    }


    /**
     * List<Entity> -> List<VO>
     */
    public static List<UserLikeStartInfoVO> fromEntityList(List<UserLikeStartInfo> list) {

        if (list == null) {
            return null;
        }

        return list.stream()
                .map(UserLikeStartInfoVO::fromEntity)
                .collect(Collectors.toList());
    }

}