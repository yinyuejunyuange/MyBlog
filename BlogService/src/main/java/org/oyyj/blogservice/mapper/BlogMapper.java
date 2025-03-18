package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.oyyj.blogservice.dto.IncreaseDTO;
import org.oyyj.blogservice.pojo.Blog;

import java.util.Date;
import java.util.List;
import java.util.Map;


@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

    @Select("select *  from blog order by 0.2*watch+star*0.4+kudos*0.4 desc limit 10; ")
    List<Blog> getHotBlog();

    // 获取 每个月增加的博客数
    @Select("select DATE_FORMAT(create_time,'%Y-%m') as month, COUNT(*) as record from blog GROUP BY month " +
            "ORDER BY month;")
    List<IncreaseDTO> getIncreaseBlog();

    @Select("SELECT MIN(year) AS min_year " +
            "FROM ( " +
            "    SELECT DATE_FORMAT(create_time, '%Y-%m') AS year " +
            "    FROM blog " +
            "    GROUP BY year " +
            ") AS yearly_data")
    String gerMinDate();


}
