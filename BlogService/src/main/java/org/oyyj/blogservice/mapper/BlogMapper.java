package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.oyyj.blogservice.dto.BlogReadDTO;
import org.oyyj.blogservice.dto.BlogSearchDTO;
import org.oyyj.blogservice.dto.BlogTypeDTO;
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


    List<BlogTypeDTO> getBlogTypeList( @Param("blogIds") List<Long> blogIds);

    /**
     * 随机获取blogId
     * @param n 随机种子
     * @return
     */
    List<Long> selectBlogIdRand(@Param("n") Long n);


    /**
     * 评分查询
     * @param userId
     * @param orderWay
     * @return
     */
    List<BlogSearchDTO> selectBlogSearch( IPage<Blog> page, @Param("userid") Long userId, @Param("typeList") List<String> typeList, @Param("orderBy") String orderBy,  @Param("orderWay")String orderWay);

    /**
     * 批量处理阅读数
     * @param readDTOS
     * @return
     */
    Integer updateBlogBatch(@Param("items") List<BlogReadDTO> readDTOS);

}
