package org.oyyj.studyservice.vo.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.studyservice.pojo.Question;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class QuestionItemVO {

    private String id;

    private String title;

    private String type;

    private List<String> options;



    public static  QuestionItemVO convertQuestionItemVO(Question  question){
        ObjectMapper mapper = new ObjectMapper();
        QuestionItemVO questionItemVO = new QuestionItemVO();
        questionItemVO.setId(question.getId().toString());
        questionItemVO.setTitle(question.getQuestionText());
        Question.QuestionType byValue = Question.QuestionType.getByValue(question.getQuestionType());
        if(byValue!=null){
            questionItemVO.setType(byValue.getDesc());
        }
        List<String> strings = null;
        try {
            strings = mapper.readValue(question.getOptions(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("数据异常 选项无法转换成集合 {}",question.getOptions(),e);
            questionItemVO.setOptions(List.of()); // 返回空列表
        }
        questionItemVO.setOptions(strings);
        return questionItemVO;
    }

}
