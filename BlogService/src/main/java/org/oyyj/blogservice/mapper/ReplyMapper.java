package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.oyyj.blogservice.pojo.Reply;

import java.util.Date;

@Mapper
public interface ReplyMapper extends BaseMapper<Reply> {

    /**
     * 查询近一个月的回复增加数量
     * @param startDate 开始日期
     * @return 回复增加数量
     */
    @Select("SELECT COUNT(*) FROM t_reply WHERE create_time >= #{startDate} AND is_delete = 0")
    Long countRecentReplies(@Param("startDate") Date startDate);

}
