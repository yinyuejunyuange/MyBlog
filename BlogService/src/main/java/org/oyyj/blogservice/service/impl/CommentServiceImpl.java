package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.mapper.CommentMapper;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.service.ICommentService;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {
}
