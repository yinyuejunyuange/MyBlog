package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.mapper.ReplyMapper;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.service.IReplyService;
import org.springframework.stereotype.Service;

@Service
public class ReplyServiceImpl extends ServiceImpl<ReplyMapper, Reply> implements IReplyService {
}
