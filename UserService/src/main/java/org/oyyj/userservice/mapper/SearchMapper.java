package org.oyyj.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.oyyj.userservice.dto.HotSearchDTO;
import org.oyyj.userservice.pojo.Search;

import java.util.List;

@Mapper
public interface SearchMapper extends BaseMapper<Search> {

    @Select("select content ,COUNT(content) from search group by content order by COUNT(content) desc limit 6 ") // 按照搜索数量进行查找
    List<HotSearchDTO> getHotSearch();

}
