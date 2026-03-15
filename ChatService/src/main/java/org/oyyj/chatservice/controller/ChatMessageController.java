package org.oyyj.chatservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.oyyj.chatservice.pojo.vo.BlackListVO;
import org.oyyj.chatservice.pojo.vo.ChatMsgVO;
import org.oyyj.chatservice.pojo.dto.ChatDialogDTO;
import org.oyyj.chatservice.pojo.vo.DialogCreateVO;
import org.oyyj.chatservice.service.BlacklistService;
import org.oyyj.chatservice.service.ChatDialogService;
import org.oyyj.chatservice.service.IChatMessageService;
import org.oyyj.chatservice.service.UserChatInfoService;
import org.oyyj.mycommon.annotation.RequestUser;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/myBlog/chat")
public class ChatMessageController {

    @Autowired
    private UserChatInfoService userChatInfoService;

    @Autowired
    private IChatMessageService chatMessageService;

    @Autowired
    private ChatDialogService chatDialogService;

    @Autowired
    private BlacklistService blacklistService;

    /**
     * 创建对话并返回dialogId
     */
    @GetMapping("/snowId")
    public ResultUtil<DialogCreateVO> getSnowId(@RequestUser LoginUser loginUser,
                                                @RequestParam("toUserId") String toUserId){

        Long dialogId = chatDialogService
                .getOrCreateDialog(loginUser.getUserId(), Long.parseLong(toUserId));

        return userChatInfoService.getSnowId(loginUser,toUserId,dialogId);
    }


    /**
     * 添加黑名单
     */
    @PostMapping("/addBlacklist")
    public ResultUtil<Boolean> addBlacklist(@RequestUser LoginUser loginUser,
                                            @RequestParam Long blackUserId){

        blacklistService.addBlacklist(loginUser.getUserId(),blackUserId);

        return ResultUtil.success(true);
    }


    /**
     * 移除黑名单
     */
    @DeleteMapping("/removeBlacklist")
    public ResultUtil<Boolean> removeBlacklist(@RequestUser LoginUser loginUser,
                                               @RequestParam Long blackUserId){

        blacklistService.removeBlacklist(loginUser.getUserId(),blackUserId);

        return ResultUtil.success(true);
    }


    /**
     * 隐藏对话
     */
    @PostMapping("/hideDialog")
    public ResultUtil<Boolean> hideDialog(@RequestUser LoginUser loginUser,
                                          @RequestParam String dialogId){

        userChatInfoService.hideDialog(loginUser.getUserId(),dialogId);

        return ResultUtil.success(true);
    }


    /**
     * 显示对话
     */
    @PostMapping("/showDialog")
    public ResultUtil<Boolean> showDialog(@RequestUser LoginUser loginUser,
                                          @RequestParam String dialogId){

        userChatInfoService.showDialog(loginUser.getUserId(),dialogId);

        return ResultUtil.success(true);
    }


    /**
     * 设置免打扰
     */
    @PostMapping("/disturb")
    public ResultUtil<Boolean> disturb(@RequestUser LoginUser loginUser,
                                       @RequestParam String dialogId,
                                       @RequestParam Integer disturb){

        userChatInfoService.setDisturb(loginUser.getUserId(),dialogId,disturb);

        return ResultUtil.success(true);
    }


    /**
     * 获取所有对话菜单
     */
    @GetMapping("/chatDialogs")
    public ResultUtil<List<ChatDialogDTO>> chatDialogs(@RequestUser LoginUser loginUser){

        return chatDialogService.dialogList(loginUser.getUserId());
    }

    /**
     * 对话详情
     * @param loginUser
     * @param dialogId
     * @param lastMsgId
     * @return
     */
    @GetMapping("/messageList")
    public ResultUtil<List<ChatMsgVO>>  messageList(@RequestUser LoginUser loginUser ,
                                                    @RequestParam("dialogId")String dialogId,
                                                    @RequestParam(value = "lastMsgId",required = false)String lastMsgId){
        return ResultUtil.success(chatMessageService.messageList(loginUser,dialogId,lastMsgId));
    }

    /**
     * 清空所有未读记录
     * @param loginUser
     * @return
     */
    @PutMapping("/cleanUnRead")
    public ResultUtil<Boolean> cleanUnRead(@RequestUser LoginUser loginUser,
                                           @RequestParam(value = "dialogId",required = false) String dialogId){
        return ResultUtil.success(chatMessageService.readMsg(loginUser,dialogId));
    }

    /**
     * 查看用户的配置信息
     * @param loginUser
     * @return
     */
    @GetMapping("/chatSetting")
    public ResultUtil<Integer>  getChatSetting(@RequestUser LoginUser loginUser){
        return chatMessageService.isUserAllow(loginUser);
    }

    /**
     * 修改用户的配置信息
     * @param loginUser
     * @param allowStrange
     * @return
     */
    @PutMapping("/chatSetting")
    public ResultUtil<Integer> updateChatSetting(@RequestUser LoginUser loginUser,@RequestParam("allowStrange") Integer  allowStrange){
        return chatMessageService.updateUserAllow(loginUser,allowStrange);
    }


    /**
     * 分页获取列表黑名单信息
     * @param loginUser
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/blackList")
    public ResultUtil<Page<BlackListVO>> blackList(@RequestUser LoginUser loginUser,
                                                   @RequestParam("pageNum") Integer pageNum,
                                                   @RequestParam("pageSize")Integer pageSize){

        return blacklistService.listBlackUser(loginUser.getUserId(),pageNum,pageSize);

    }



}
