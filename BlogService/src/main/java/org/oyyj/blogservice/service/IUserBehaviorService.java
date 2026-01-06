package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.pojo.UserBehavior;
import org.oyyj.mycommon.common.BehaviorEnum;

public interface IUserBehaviorService extends IService<UserBehavior> {
    /**
     * 用户阅读博客并添加计数
     * @param blogId
     * @param userId
     * @return
     * @throws Exception
     */
    boolean incrementReadCount(Long blogId, Long userId) throws Exception;

    /**
     * 用户除阅读外的其他行为
     * @param blogId
     * @param userId
     * @param behaviorEnum
     * @return
     * @throws Exception
     */
    boolean userBehaviorBlog(Long blogId, Long userId, BehaviorEnum behaviorEnum) throws Exception;
}
