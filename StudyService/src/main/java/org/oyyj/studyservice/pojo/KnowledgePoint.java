package org.oyyj.studyservice.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.oyyj.studyservice.dto.knowledgePoint.InterviewQuestionsDTO;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 知识点表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_point")
@Slf4j
public class KnowledgePoint {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private Date updateTime;

    @TableField("title")
    private String title;             // 题目

    @TableField("recommended_answer")
    private String recommendedAnswer; // 推荐回答

    @TableField("level")
    private Integer level;

    @TableField("type")
    private String type;

    /**
     * 相关面试问题
     * @see org.oyyj.studyservice.dto.knowledgePoint.InterviewQuestionsDTO
     */
    @TableField("related_questions")
    private String relatedQuestions;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;         // 是否删除 1已删除 0未删除


    @Getter
    public  enum LevelEnum{
        SIMPLE(0,"简单"),
        MEDIUM(1,"中等"),
        DEFFICUlTE(2,"困难");

        private final Integer value;
        private final String desc;
        LevelEnum(Integer value,String desc){
            this.value = value;
            this.desc = desc;
        }

        public static LevelEnum getByValue(int value){
            for (LevelEnum levelEnum : LevelEnum.values()) {
                if (levelEnum.value == value){
                    return levelEnum;
                }
            }
            return null;
        }

        public static LevelEnum getByDesc(String desc){
            for (LevelEnum levelEnum : LevelEnum.values()) {
                if (levelEnum.desc == desc){
                    return levelEnum;
                }
            }
            return null;
        }
    }

    public KnowledgePointDTO entityToDTO(KnowledgePoint entity) {

        ObjectMapper mapper = new ObjectMapper();
        KnowledgePointDTO knowledgePointDTO = new KnowledgePointDTO();
        BeanUtils.copyProperties(entity, knowledgePointDTO);
        knowledgePointDTO.setId(String.valueOf(entity.getId()));
        List<InterviewQuestionsDTO> interviewQuestionsDTOS = new ArrayList<>();
        try {
            if(entity.getRelatedQuestions()!=null){
                interviewQuestionsDTOS = mapper.readValue(entity.getRelatedQuestions(), new TypeReference<List<InterviewQuestionsDTO>>() {
                });
            }
        } catch (JsonProcessingException e) {
            log.error("知识点相关面试题转换失败，数据如下：{}",entity.getRelatedQuestions(),e);
        }
        knowledgePointDTO.setRelatedQuestions(interviewQuestionsDTOS);
        KnowledgePoint.LevelEnum byValue = KnowledgePoint.LevelEnum.getByValue(entity.getLevel());
        if(byValue==null){
            log.error("难度等级不正确{}",entity.getLevel());
        }else{
            knowledgePointDTO.setLevel(byValue.getDesc());
        }
        if(entity.getType()!=null && Strings.isNotBlank(entity.getType()) ){
            knowledgePointDTO.setType(Arrays.asList(entity.getType().split(",")));
        }

        return knowledgePointDTO;
    }

}