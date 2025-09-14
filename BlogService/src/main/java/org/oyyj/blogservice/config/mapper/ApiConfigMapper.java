package org.oyyj.blogservice.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.config.pojo.ApiConfig;

@Mapper
public interface ApiConfigMapper extends BaseMapper<ApiConfig> {

}
