package org.oyyj.chatservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.UserChatInfo;
import org.oyyj.chatservice.pojo.vo.DialogCreateVO;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserChatInfoService extends IService<UserChatInfo> {

    ResultUtil<DialogCreateVO> getSnowId(LoginUser loginUser , String toUserId, Long dialogId);


    void hideDialog(Long userId,String dialogId);

    void showDialog(Long userId,String dialogId);

    void setDisturb(Long userId,String dialogId,Integer disturb);

}
