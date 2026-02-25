package org.oyyj.studyservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.studyservice.dto.UserExamRecord.UserAnswerDetailDTO;
import org.oyyj.studyservice.pojo.UserExamRecord;

import java.util.List;

@Mapper
public interface UserExamRecordMapper extends BaseMapper<UserExamRecord> {

    // 插入明细记录
    int insertUserAnswerDetail(UserAnswerDetailDTO detail);

    // 根据批次ID查询明细
    List<UserAnswerDetailDTO> selectUserAnswerDetailByRecordId(@Param("recordId") Long recordId);

    // 根据ID更新
    int updateUserAnswerDetailById(UserAnswerDetailDTO detail);

}
