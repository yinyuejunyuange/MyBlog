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
 * 试题表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("question")
public class Question {
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

    @TableField("question_type")
    private String questionType;      // 题型：single_choice-单选, multiple_choice-多选, true_false-判断

    @TableField("question_text")
    private String questionText;      // 题干

    @TableField("options")
    private String options;           // 选项（JSON字符串）

    @TableField("answer")
    private String answer;            // 答案（JSON字符串）

    @TableField("knowledge_point_id")
    private Long knowledgePointId;    // 关联知识点ID

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;         // 是否删除 1已删除 0未删除

    @TableField("explanation")
    private String explanation;       // 题目讲解


    @Getter
    public enum QuestionType {
        SINGLE("single" , "单选题"),
        TRUE_FALSE("true-false","判断题"),
        multiple("multiple","多选题");

        private final String value;
        private final String desc;
        QuestionType(String value , String desc) {
            this.value = value;
            this.desc = desc;
        }

        public static QuestionType getByValue(String value) {
            for (QuestionType item : QuestionType.values()) {
                if (item.value.equals(value)) {
                    return item;
                }
            }
            return null;
        }

        public static QuestionType getByDesc(String desc) {
            for (QuestionType item : QuestionType.values()) {
                if (item.desc.equals(desc)) {
                    return item;
                }
            }
            return null;
        }
    }

}
