package org.oyyj.chatservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.chatservice.pojo.ChatDialog;
import org.oyyj.chatservice.pojo.dto.ChatDialogDTO;
import org.oyyj.mycommonbase.utils.ResultUtil;

import java.util.List;

public interface ChatDialogService extends IService<ChatDialog> {

    /**
     * 获取或创建会话
     */
    Long getOrCreateDialog(Long userA, Long userB);

    ResultUtil<List<ChatDialogDTO>> dialogList(Long userId);

}
