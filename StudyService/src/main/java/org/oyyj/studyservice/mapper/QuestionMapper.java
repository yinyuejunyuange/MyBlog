package org.oyyj.studyservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.studyservice.pojo.Question;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
