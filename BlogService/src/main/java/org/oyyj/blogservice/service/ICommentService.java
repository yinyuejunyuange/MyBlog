package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.ReadCommentDTO;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.mycommonbase.common.auth.LoginUser;

import java.util.List;

public interface ICommentService extends IService<Comment> {

    List<ReadCommentDTO> getBlogComment(String blogId, LoginUser loginUser,Integer pageNum);

    Boolean changeCommentKudos(Long commentId,Integer isAdd);



}
