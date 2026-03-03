package org.oyyj.studyservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.studyservice.pojo.KnowledgeBase;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    List<String> listAllBaseTypes();

}
