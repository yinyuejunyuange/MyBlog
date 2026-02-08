package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.ReadReplyDTO;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.mycommonbase.common.auth.LoginUser;

import java.util.List;

public interface IReplyService extends IService<Reply> {

    Boolean changeReplyKudos(Long replyId,Integer isAdd);

    List<ReadReplyDTO> getReply(Long commentId, LoginUser loginUser, Integer pageNum);
}
