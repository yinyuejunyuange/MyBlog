package org.oyyj.chatservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.oyyj.chatservice.pojo.UserFollowInfo;
import org.oyyj.chatservice.pojo.UserLikeStartInfo;
import org.oyyj.chatservice.pojo.vo.UserFollowInfoVO;
import org.oyyj.chatservice.pojo.vo.UserLikeStartInfoVO;
import org.oyyj.chatservice.service.UserFollowInfoService;
import org.oyyj.chatservice.service.UserLikeStartInfoService;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/myBlog/follow")
public class FollowController {

    @Autowired
    private UserFollowInfoService userFollowInfoService;

    @Autowired
    private UserLikeStartInfoService  userLikeStartInfoService;


    // 查询列表
    /**
     * 查询粉丝列表信息
     * @param loginUser
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/fans")
    public ResultUtil<Page<UserFollowInfoVO>> fans(@RequestUser LoginUser loginUser,
                                                   @RequestParam("pageNum") Integer pageNum,
                                                   @RequestParam("pageSize")Integer pageSize){
        return ResultUtil.success(userFollowInfoService.pageFansList(loginUser.getUserId(),pageNum,pageSize));
    }

    /**
     * 分页查询 赞和收藏的信息
     * @param loginUser
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/likeStar")
    public ResultUtil<Page<UserLikeStartInfoVO>> likeStar(@RequestUser LoginUser loginUser,
                                                          @RequestParam("pageNum") Integer pageNum,
                                                          @RequestParam("pageSize")Integer pageSize){
        return ResultUtil.success(userLikeStartInfoService.pageBehaviorList(loginUser.getUserId(),null , pageNum,pageSize));
    }
    // 删除信息

    /**
     *
     * @param loginUser
     * @param userId 关注者ID
     * @return
     */
    @DeleteMapping("/fans")
    public ResultUtil<String> deleteFanInfo(@RequestUser LoginUser loginUser,
                                            @RequestParam("userId") String userId){
        userFollowInfoService.deleteFollow(Long.parseLong(userId),loginUser.getUserId());
        return ResultUtil.success("删除信息成功");
    }


    /**
     * 删除行为信息
     * @param behaviorId
     * @return
     */
    @DeleteMapping("/likeStar")
    public ResultUtil<String> deleteLikeStar(@RequestParam("behaviorId")String behaviorId ){
        userLikeStartInfoService.remove(Wrappers.<UserLikeStartInfo>lambdaQuery()
                .eq(UserLikeStartInfo::getId,Long.parseLong(behaviorId)));
        return ResultUtil.success("删除信息成功");
    }




    /**
     * 以下接口用于feign
     */

    // 新增信息

    /**
     * 新增粉丝
     * @param userId  关注者
     * @param followId  被关注者
     */
    @PutMapping("/fans")
    public void addFansInfo(@RequestParam("userId")  String userId,@RequestParam("followId") String followId){
        userFollowInfoService.addFollow(Long.parseLong(userId),Long.parseLong(followId));
    }

    /**
     *
     * @param userId 作者ID
     * @param behaviorId 操作者ID
     * @param targetId 操作目标ID
     * @param targetType  目标类别
     * @param behavior 操作行为
     */
    @PutMapping("/likeStar")
    public void addLikeStar(@RequestParam("userId")  String userId,
                            @RequestParam("behaviorId") String behaviorId,
                            @RequestParam("targetId") String targetId,
                            @RequestParam("targetType")  Integer targetType,
                            @RequestParam("behavior") Integer behavior){
        userLikeStartInfoService.addBehavior(Long.parseLong(userId),
                Long.parseLong(behaviorId),
                Long.parseLong(targetId),
                targetType,
                behavior);
    }

    // 删除信

    /**
     * 脱粉
     * @param userId
     * @param followId
     */
    @DeleteMapping("/unfollow")
    public void unfollow(@RequestParam("userId")  String userId , @RequestParam("followId")  String followId){
        userFollowInfoService.deleteFollow(Long.parseLong(userId),Long.parseLong(followId));
    }

    @DeleteMapping("/cancelLikeStar")
    public void cancelLikeStar(@RequestParam("userId")  String userId,
                               @RequestParam("targetId") String targetId,
                               @RequestParam("targetType")  Integer targetType,
                               @RequestParam("behavior") Integer behavior){
        userLikeStartInfoService.deleteBehavior(Long.parseLong(userId),Long.parseLong(targetId),targetType,behavior);
    }

}
