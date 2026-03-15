package org.oyyj.chatservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.UserFollowInfo;
import org.oyyj.chatservice.pojo.vo.UserFollowInfoVO;


public interface UserFollowInfoService extends IService<UserFollowInfo> {


    /**
     * 新增关注
     */
    boolean addFollow(Long userId, Long followUserId);

    /**
     * 取消关注
     */
    boolean deleteFollow(Long userId, Long followUserId);

    /**
     * 查询关注列表（我关注的人）
     */
    Page<UserFollowInfo> pageFollowList(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询粉丝列表
     */
    Page<UserFollowInfoVO> pageFansList(Long followUserId, Integer pageNum, Integer pageSize);

}
