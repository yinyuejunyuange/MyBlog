package org.oyyj.chatservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.oyyj.chatservice.feign.UserFeign;
import org.oyyj.chatservice.mapper.UserFollowInfoMapper;
import org.oyyj.chatservice.pojo.UserFollowInfo;
import org.oyyj.chatservice.pojo.vo.UserFollowInfoVO;
import org.oyyj.chatservice.service.UserFollowInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserFollowInfoServiceImpl
        extends ServiceImpl<UserFollowInfoMapper, UserFollowInfo>
        implements UserFollowInfoService {

    @Autowired
    private UserFeign  userFeign;

    /**
     * 新增关注
     */
    @Override
    public boolean addFollow(Long userId, Long followUserId) {

        UserFollowInfo follow = new UserFollowInfo();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        follow.setIsUserKnow(0);
        follow.setIsDelete(0);

        return save(follow);
    }

    /**
     * 删除关注
     */
    @Override
    public boolean deleteFollow(Long userId, Long followUserId) {

        return remove(new LambdaQueryWrapper<UserFollowInfo>()
                .eq(UserFollowInfo::getUserId, userId)
                .eq(UserFollowInfo::getFollowUserId, followUserId));
    }

    /**
     * 我关注的人
     */
    @Override
    public Page<UserFollowInfo> pageFollowList(Long userId,
                                               Integer pageNum,
                                               Integer pageSize) {

        Page<UserFollowInfo> page = new Page<>(pageNum, pageSize);

        Page<UserFollowInfo> pageInfo = page(page,
                new LambdaQueryWrapper<UserFollowInfo>()
                        .eq(UserFollowInfo::getUserId, userId)
                        .eq(UserFollowInfo::getIsDelete, 0)
                        .orderByDesc(UserFollowInfo::getCreatedAt));

        List<UserFollowInfo> records = pageInfo.getRecords();
        List<String> userStr = records.stream().map(item -> {
            return String.valueOf(item.getUserId());
        }).toList();
        Map<Long, String> imageInIds = userFeign.getImageInIds(userStr);
        Map<Long, String> nameInIds = userFeign.getNameInIds(userStr);

        records = records.stream()
                .peek(item -> {
                    item.setUserHead(imageInIds.get(item.getUserId()));
                    item.setUserName(nameInIds.get(item.getUserId()));
                })
                .toList();
        pageInfo.setRecords(records);
        return pageInfo;
    }

    /**
     * 我的粉丝
     */
    @Override
    public Page<UserFollowInfoVO> pageFansList(Long followUserId,
                                             Integer pageNum,
                                             Integer pageSize) {

        Page<UserFollowInfo> page = new Page<>(pageNum, pageSize);

        Page<UserFollowInfo> pageResult = page(page,
                new LambdaQueryWrapper<UserFollowInfo>()
                        .eq(UserFollowInfo::getFollowUserId, followUserId)
                        .eq(UserFollowInfo::getIsDelete, 0)
                        .orderByDesc(UserFollowInfo::getCreatedAt));

        List<UserFollowInfo> records = pageResult.getRecords();

        Page<UserFollowInfoVO> result = new Page<>();
        result.setTotal(pageResult.getTotal());
        if(records.isEmpty()){
            return result;
        }
        List<String> userIds = records.stream().map(item-> {
            return String.valueOf(item.getUserId());
        }).toList();

        Map<Long, String> imageInIds = userFeign.getImageInIds(userIds);
        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);

        List<UserFollowInfoVO> list = records.stream().map(item -> {

            if (imageInIds.containsKey(item.getUserId())) {
                item.setUserHead(imageInIds.get(item.getUserId()));
            }
            if (nameInIds.containsKey(item.getUserId())) {
                item.setUserName(nameInIds.get(item.getUserId()));
            }

            return UserFollowInfoVO.fromEntity(item);
        }).toList();


        result.setRecords(list);
        return result;

    }

}