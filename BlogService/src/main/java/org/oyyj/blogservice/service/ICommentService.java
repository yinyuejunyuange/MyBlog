package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.ReadCommentDTO;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.vo.commet.CommentResultVO;
import org.oyyj.mycommonbase.common.auth.LoginUser;

import java.util.Date;
import java.util.List;

public interface ICommentService extends IService<Comment> {

    CommentResultVO getBlogComment(String blogId, LoginUser loginUser, Date lastTime, String lastId);

    Boolean changeCommentKudos(Long commentId,Integer isAdd,LoginUser loginUser);



}
