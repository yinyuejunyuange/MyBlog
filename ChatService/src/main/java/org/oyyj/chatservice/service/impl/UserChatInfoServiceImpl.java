package org.oyyj.chatservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.chatservice.feign.UserFeign;
import org.oyyj.chatservice.mapper.UserChatInfoMapper;
import org.oyyj.chatservice.pojo.UserChatInfo;
import org.oyyj.chatservice.pojo.vo.DialogCreateVO;
import org.oyyj.chatservice.service.UserChatInfoService;
import org.oyyj.mycommon.utils.SnowflakeUtil;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserChatInfoServiceImpl
        extends ServiceImpl<UserChatInfoMapper, UserChatInfo>
        implements UserChatInfoService {

    @Autowired
    private SnowflakeUtil snowflakeUtil;
    @Autowired
    private UserFeign userFeign;

    @Override
    public ResultUtil<DialogCreateVO> getSnowId(LoginUser loginUser, String toUserId, Long dialogId) {
        UserChatInfo one = getOne(Wrappers.<UserChatInfo>lambdaQuery()
                .eq(UserChatInfo::getUserId, loginUser.getUserId()).eq(UserChatInfo::getDialogId, dialogId)
        );

        DialogCreateVO dialogCreateVO = new DialogCreateVO();

        if (one == null) {
            one = new UserChatInfo();
            one.setUserId(loginUser.getUserId());
            one.setDialogId(dialogId);
            one.setUserName(loginUser.getUserName());
            try {
                save(one);
            } catch (DuplicateKeyException e) {
                log.error(e.getMessage());
            }
        }


        UserChatInfo two = getOne(Wrappers.<UserChatInfo>lambdaQuery()
                .eq(UserChatInfo::getUserId, Long.parseLong(toUserId)).eq(UserChatInfo::getDialogId, dialogId)
        );

        Map<Long, String> nameInIds = userFeign.getNameInIds(Collections.singletonList(toUserId));

        if (two == null) {
            two = new UserChatInfo();
            two.setUserId(Long.valueOf(toUserId));
            two.setDialogId(dialogId);
            if(nameInIds!=null &&  nameInIds.containsKey(Long.valueOf(toUserId))){
                two.setUserName(nameInIds.get(Long.valueOf(toUserId)));
            }

            try {
                save(two);
            } catch (DuplicateKeyException e) {
                log.error(e.getMessage());
            }
        }

        dialogCreateVO.setDialogId(String.valueOf(dialogId));
        dialogCreateVO.setSnowflakeId(snowflakeUtil.getSnowflakeId());
        return ResultUtil.success( dialogCreateVO );

    }


    /**
     * 隐藏对话
     */
    @Override
    public void hideDialog(Long userId,String dialogId){

        update(Wrappers.<UserChatInfo>lambdaUpdate()
                .eq(UserChatInfo::getUserId,userId)
                .eq(UserChatInfo::getDialogId,dialogId)
                .set(UserChatInfo::getIsVisible,0)
        );

    }

    /**
     * 显示对话
     */
    @Override
    public void showDialog(Long userId,String dialogId){

        update(Wrappers.<UserChatInfo>lambdaUpdate()
                .eq(UserChatInfo::getUserId,userId)
                .eq(UserChatInfo::getDialogId,dialogId)
                .set(UserChatInfo::getIsVisible,1)
        );

    }


    /**
     * 免打扰
     */
    @Override
    public void setDisturb(Long userId,String dialogId,Integer disturb){

        update(Wrappers.<UserChatInfo>lambdaUpdate()
                .eq(UserChatInfo::getUserId,userId)
                .eq(UserChatInfo::getDialogId,dialogId)
                .set(UserChatInfo::getIsDisturb,disturb)
        );

    }

}
