package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.blogservice.pojo.TypeTable;
import org.oyyj.blogservice.vo.admin.BlogTypeVO;
import org.oyyj.blogservice.vo.type.TypeTableVO;

import java.util.List;

@Mapper
public interface TypeTableMapper extends BaseMapper<TypeTable> {

    List<TypeTable> findTypesByBlogId(@Param("blogId")Long blogId);

    /**
     * 某类别 / 所有博客数量
     * @param typeId
     * @return
     */
    List<BlogTypeVO> findBlogTypeNum(@Param("typeId") Long typeId);

}
