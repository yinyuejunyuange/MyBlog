package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.ReadReplyDTO;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.vo.reply.ReplyResultVO;
import org.oyyj.mycommonbase.common.auth.LoginUser;

import java.util.Date;
import java.util.List;

public interface IReplyService extends IService<Reply> {

    Boolean changeReplyKudos(Long replyId, Integer isAdd,LoginUser loginUser);

    ReplyResultVO getReply(Long commentId, LoginUser loginUser, String lastId, Date lastTime);
}
