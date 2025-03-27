package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.pojo.CommentReport;

@Mapper
public interface CommentReportMapper extends BaseMapper<CommentReport> {
}
