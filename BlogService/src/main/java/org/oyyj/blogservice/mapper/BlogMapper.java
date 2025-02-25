package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.pojo.Blog;


@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

}
