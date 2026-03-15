package org.oyyj.chatservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.UserLikeStartInfo;
import org.oyyj.chatservice.pojo.vo.UserLikeStartInfoVO;

public interface UserLikeStartInfoService extends IService<UserLikeStartInfo> {

    /**
     * 新增点赞 / 收藏
     */
    boolean addBehavior(Long userId,
                        Long behaviorId,
                        Long targetId,
                        Integer targetType,
                        Integer behaviour);

    /**
     * 删除行为
     */
    boolean deleteBehavior(Long userId,
                           Long targetId,
                           Integer targetType,
                           Integer behaviour);

    /**
     * 分页查询用户点赞/收藏
     */
    Page<UserLikeStartInfoVO> pageBehaviorList(Long userId,
                                               Integer behaviour,
                                               Integer pageNum,
                                               Integer pageSize);
}
