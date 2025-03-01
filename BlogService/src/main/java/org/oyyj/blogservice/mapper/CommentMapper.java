package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.pojo.Comment;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
