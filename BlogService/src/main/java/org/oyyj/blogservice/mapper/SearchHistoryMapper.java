package org.oyyj.blogservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.oyyj.blogservice.dto.SearchCountDTO;
import org.oyyj.blogservice.pojo.SearchHistory;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {

    /**
     * 统计指定时间后各标准化搜索词的出现次数
     * @param since 起始时间
     * @param offset 分页偏移量
     * @param limit 分页条数
     * @return 结果列表：每个元素是 Map，包含 queryNorm（搜索词）、count（次数）
     */
    List<SearchCountDTO> countByNormSince(
            @Param("since") Date since,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 根据标准化词查询所有搜过该词的用户ID（去重）
     * @param word 标准化搜索词
     * @return 去重后的用户ID列表
     */
    List<Long> findUserIdsByQueryNorm(@Param("word") String word);

    /**
     * 根据用户ID列表，查询这些用户搜过的其他标准化词（排除指定词）并统计次数
     * @param userIds 用户ID列表
     * @param excludeWords 要排除的词列表
     * @param offset 分页偏移量
     * @param limit 分页条数
     * @return 结果列表：每个Map包含queryNorm（标准化词）、count（次数）
     */
    List<SearchCountDTO> findOtherWordsByUserIds(
            @Param("userIds") List<String> userIds,
            @Param("excludeWords") List<String> excludeWords,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 获取用户的前几条记录
     * @param userId
     * @return
     */
    List<SearchHistory> findTop5ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 获取热门推荐信息
     * @param userIds
     * @return
     */
    List<SearchHistory> findTopByUserIdsOrderByCreatedAtDesc(@Param("userIds") List<Long> userIds);
}
