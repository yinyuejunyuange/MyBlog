package org.oyyj.chatservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.chatservice.feign.BlogFeign;
import org.oyyj.chatservice.feign.UserFeign;
import org.oyyj.chatservice.mapper.UserLikeStartInfoMapper;
import org.oyyj.chatservice.pojo.UserLikeStartInfo;
import org.oyyj.chatservice.pojo.vo.UserLikeStartInfoVO;
import org.oyyj.chatservice.service.UserLikeStartInfoService;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserLikeStartInfoServiceImpl
        extends ServiceImpl<UserLikeStartInfoMapper, UserLikeStartInfo>
        implements UserLikeStartInfoService {


    @Autowired
    private BlogFeign blogFeign;


    @Autowired
    private UserFeign userFeign;
    /**
     * 新增点赞 / 收藏
     */
    @Override
    public boolean addBehavior(Long userId,
                               Long behaviorId,
                               Long targetId,
                               Integer targetType,
                               Integer behaviour) {

        UserLikeStartInfo like = new UserLikeStartInfo();
        like.setUserId(userId);
        like.setBehaviorId(behaviorId);
        like.setTargetId(targetId);
        like.setTargetType(targetType);
        like.setBehaviour(behaviour);

        return save(like);
    }

    /**
     * 删除点赞 / 收藏
     */
    @Override
    public boolean deleteBehavior(Long userId,
                                  Long targetId,
                                  Integer targetType,
                                  Integer behaviour) {

        return remove(new LambdaQueryWrapper<UserLikeStartInfo>()
                .eq(UserLikeStartInfo::getUserId, userId)
                .eq(UserLikeStartInfo::getTargetId, targetId)
                .eq(UserLikeStartInfo::getTargetType, targetType)
                .eq(UserLikeStartInfo::getBehaviour, behaviour));
    }

    /**
     * 分页查询点赞 / 收藏
     */
    @Override
    public Page<UserLikeStartInfoVO> pageBehaviorList(Long userId,
                                                      Integer behaviour,
                                                      Integer pageNum,
                                                      Integer pageSize) {

        Page<UserLikeStartInfo> page = new Page<>(pageNum, pageSize);

        Page<UserLikeStartInfo> pageResult = page(page,
                new LambdaQueryWrapper<UserLikeStartInfo>()
                        .eq(UserLikeStartInfo::getBehaviorId, userId)
                        .eq( behaviour != null, UserLikeStartInfo::getBehaviour, behaviour)
                        .orderByDesc(UserLikeStartInfo::getCreatedAt));

        List<UserLikeStartInfo> records = pageResult.getRecords();

        Page<UserLikeStartInfoVO> result = new Page<>();

        result.setTotal(page.getTotal());

        if(records.isEmpty()){
            return result;
        }

        List<String> blogIds = records.stream()
                .filter(item -> Objects.nonNull(item.getTargetType()) && item.getTargetType().equals(1))
                .map(item-> String.valueOf(item.getTargetId()))
                .toList();

        Map<Long, String> titleMap = blogFeign.blogTitleByStrIds(blogIds);


        List<String> commentIds = records.stream()
                .filter(item -> Objects.nonNull(item.getTargetType()) && item.getTargetType().equals(2))
                .map(item-> String.valueOf(item.getTargetId()))
                .toList();

        Map<Long, String> commentMap = blogFeign.commentsByStrIds(commentIds);

        List<String> replyIds = records.stream()
                .filter(item -> Objects.nonNull(item.getTargetType()) && item.getTargetType().equals(3))
                .map(item-> String.valueOf(item.getTargetId()))
                .toList();

        Map<Long, String> replyMap = blogFeign.replyByStrIds(replyIds);

        List<String> behaviorUserIds = records.stream().map(item-> {
            return String.valueOf(item.getUserId());
        }).toList();

        Map<Long, String> nameInIds = userFeign.getNameInIds(behaviorUserIds);

        Map<Long, String> imageInIds = userFeign.getImageInIds(behaviorUserIds);

        List<UserLikeStartInfoVO> list = records.stream().map(item -> {
            if (nameInIds.containsKey(item.getUserId())) {
                item.setBehaviorUserName(nameInIds.get(item.getUserId()));
            }
            if (imageInIds.containsKey(item.getUserId())) {
                item.setBehaviorUserHead(imageInIds.get(item.getUserId()));
            }
            if (titleMap.containsKey(item.getTargetId())) {
                item.setTargetContent(titleMap.get(item.getTargetId()));
            }
            if (commentMap.containsKey(item.getTargetId())) {
                item.setTargetContent(commentMap.get(item.getTargetId()));
            }
            if (replyMap.containsKey(item.getTargetId())) {
                item.setTargetContent(replyMap.get(item.getTargetId()));
            }

            return UserLikeStartInfoVO.fromEntity(item);
        }).toList();


        result.setRecords(list);
        return result;
    }
}