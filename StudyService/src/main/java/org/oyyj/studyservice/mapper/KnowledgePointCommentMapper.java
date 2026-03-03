package org.oyyj.studyservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.studyservice.dto.knowledgePointComment.CommentCountDTO;
import org.oyyj.studyservice.pojo.KnowledgePointComment;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgePointCommentMapper extends BaseMapper<KnowledgePointComment> {


    List<CommentCountDTO> getCommentNumByCommentId(@Param("commentIds") List<Long> commentIds);


}