package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.oyyj.blogservice.pojo.Blog;

import java.util.List;


@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

    @Select("select *  from blog order by 0.2*watch+star*0.4+kudos*0.4 desc limit 10; ")
    List<Blog> getHotBlog();

}
