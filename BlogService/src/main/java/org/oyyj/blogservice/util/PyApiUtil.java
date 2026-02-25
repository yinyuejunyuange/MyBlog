package org.oyyj.blogservice.util;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.oyyj.blogservice.dto.BlogSummaryRequestDTO;
import org.oyyj.blogservice.dto.TextToxicAnalysisResultDTO;
import org.oyyj.blogservice.mapper.CommentMapper;
import org.oyyj.blogservice.mapper.ReplyMapper;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * py服务相关接口
 */
@Component
@Async
@Slf4j
public class PyApiUtil {

    @Value("${app.normalize.service.toxic-url}")
    private String toxicUrl;

    @Value("${app.normalize.service.blog-summary-url}")
    private String blogSummaryUrl;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReplyMapper replyMapper;

    private static final ObjectMapper mapper = new ObjectMapper();

    static{
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 获取评论的结果 存储在
     * @param text 评论词
     * @param id 对应ID
     * @param type 类别 1 是评论 0 是回复
     */
    public void getCommentToxicPredict(String text, Long id, Integer type){
        RestTemplate rest = new RestTemplate();
        try {
            String resp = rest.getForObject(toxicUrl + "?text={text}", String.class, text);
            JsonNode node = mapper.readTree(resp);
// 3. 初始化结果实体类
            TextToxicAnalysisResultDTO result = new TextToxicAnalysisResultDTO();

            // 4. 解析各个字段（包含类型转换和空值容错）
            // 4.1 解析 input_text（字符串）
            if (node.has("input_text")) {
                result.setInputText(node.get("input_text").asText());
            }

            // 4.2 解析 is_toxic（字符串转 Integer，适配实体类的 Integer 类型）
            if (node.has("is_toxic")) {
                String isToxicStr = node.get("is_toxic").asText();
                // 容错：空字符串/非数字时设为 0
                result.setIsToxic(isToxicStr != null && !isToxicStr.isEmpty() ? Integer.parseInt(isToxicStr) : 0);
            }

            // 4.3 解析 p_toxic（Double 转 BigDecimal，适配实体类的 BigDecimal 类型）
            if (node.has("p_toxic")) {
                double pToxicDouble = node.get("p_toxic").asDouble();
                result.setPToxic(BigDecimal.valueOf(pToxicDouble));
            }

            // 4.4 解析 mul_type（JSON 数组转 List<String>，再拼接为字符串，适配实体类的 String 类型）
            if (node.has("mul_type") && node.get("mul_type").isArray()) {
                // 解析 JSON 数组为 List<String>
                List<String> mulTypeList = mapper.convertValue(
                        node.get("mul_type"),
                        new TypeReference<List<String>>() {}
                );
                result.setMulType(mulTypeList);
            }

            // 4.5 解析 p_topic（JSON 对象转 Map<String, Double>，再转 JSON 字符串，适配实体类的 String 类型）
            if (node.has("p_topic") && node.get("p_topic").isObject()) {
                // 解析 JSON 对象为 Map<String, Double>
                Map<String, BigDecimal> pTopicMap = mapper.convertValue(
                        node.get("p_topic"),
                        new TypeReference<Map<String, BigDecimal>>() {}
                );
                // 转成 JSON 字符串存储（适配实体类的 String 类型）
                result.setPTopic(pTopicMap);
            }

            if(YesOrNoEnum.YES.getCode().equals(type)){
                commentMapper.update(Wrappers.<Comment>lambdaUpdate()
                        .eq(Comment::getId, id)
                        // 更新是否有毒性（is_toxic）
                        .set(result.getIsToxic() != null, Comment::getIsToxic, result.getIsToxic())
                        // 更新毒性概率（p_toxic）
                        .set(result.getPToxic() != null, Comment::getPToxic, result.getPToxic())
                        // 补全：更新攻击类别（mul_type）
                        .set( Comment::getMulType, result.getMulType() != null?String.join(",", result.getMulType()):"")
                        // 补全：更新各类别毒性概率（p_topic）
                        .set(result.getPTopic() != null, Comment::getPTopic,  mapper.writeValueAsString(result.getPTopic())));
            }else{
                replyMapper.update(Wrappers.<Reply>lambdaUpdate()
                        .eq(Reply::getId, id)
                        // 更新是否有毒性（is_toxic）
                        .set(result.getIsToxic() != null, Reply::getIsToxic, result.getIsToxic())
                        // 更新毒性概率（p_toxic）
                        .set(result.getPToxic() != null, Reply::getPToxic, result.getPToxic())
                        // 补全：更新攻击类别（mul_type）
                        .set( Reply::getMulType, result.getMulType() != null?String.join(",", result.getMulType()):"")
                        // 补全：更新各类别毒性概率（p_topic）
                        .set(result.getPTopic() != null, Reply::getPTopic,  mapper.writeValueAsString(result.getPTopic())));
            }

        } catch (Exception e) {
            log.error("调用攻击性预测失败", e);
        }
    }

    /**
     * 调用 Python 服务生成博客简介（Markdown → SnowNLP）
     * @param mdContent Markdown 原文
     */
    public CompletableFuture<String> getBlogSummaryFromPy( String mdContent) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 1. 构造请求体
            BlogSummaryRequestDTO req = new BlogSummaryRequestDTO();
            req.setMd_content(mdContent);
            req.setSentence_count(3);

            // 2. 调用 Python 接口
            String resp = restTemplate.postForObject(
                    blogSummaryUrl,
                    req,
                    String.class
            );

            // 3. 解析返回 JSON
            JsonNode node = mapper.readTree(resp);
            if (!node.has("summary")) {
                log.warn("博客简介生成失败，返回内容异常: {}", resp);
                return CompletableFuture.completedFuture("");
            }

            String summary = node.get("summary").asText();

            return CompletableFuture.completedFuture(summary);

        } catch (Exception e) {
            log.error("调用 Python 博客简介生成失败 ", e);
            return CompletableFuture.completedFuture("");
        }
    }

}
