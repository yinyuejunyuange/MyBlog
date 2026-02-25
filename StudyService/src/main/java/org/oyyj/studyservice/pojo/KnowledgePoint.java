package org.oyyj.studyservice.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * 知识点表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_point")
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

}