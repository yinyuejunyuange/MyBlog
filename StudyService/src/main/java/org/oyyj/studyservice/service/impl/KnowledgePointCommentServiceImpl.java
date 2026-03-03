package org.oyyj.studyservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.common.commonEnum.YesOrNoEnum;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePointComment.CommentCountDTO;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointCommentDTO;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointReplyDTO;
import org.oyyj.studyservice.feign.UserFeign;
import org.oyyj.studyservice.mapper.KnowledgePointCommentMapper;
import org.oyyj.studyservice.pojo.KnowledgePointComment;
import org.oyyj.studyservice.service.KnowledgePointCommentService;
import org.oyyj.studyservice.vo.knowledgeComment.KnowledgeCommentVO;
import org.oyyj.studyservice.vo.knowledgeComment.KnowledgeReplyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgePointCommentServiceImpl
        extends ServiceImpl<KnowledgePointCommentMapper, KnowledgePointComment>
        implements KnowledgePointCommentService {

    private final Integer pageSize = 20;

    private final Integer replyPageSize = 10;

    @Autowired
    private UserFeign userFeign;

    /**
     * 简单查询
     * @param knowledgeId
     * @return
     */
    @Override
    public List<KnowledgePointComment> listRootComments(Long knowledgeId) {

        return this.list(new LambdaQueryWrapper<KnowledgePointComment>()
                .eq(KnowledgePointComment::getKnowledgeId, knowledgeId)
                .isNull(KnowledgePointComment::getParentId)
                .orderByDesc(KnowledgePointComment::getCreateTime)
        );
    }

    /**
     * 一个简单的查询
     * @param rootId
     * @return
     */
    @Override
    public List<KnowledgePointComment> listReplies(Long rootId) {

        return this.list(new LambdaQueryWrapper<KnowledgePointComment>()
                .eq(KnowledgePointComment::getRootId, rootId)
                .isNotNull(KnowledgePointComment::getParentId)
                .orderByAsc(KnowledgePointComment::getCreateTime)
        );
    }

    @Override
    public boolean addComment(Long knowledgeId, String content, LoginUser loginUser) {

        KnowledgePointComment comment = new KnowledgePointComment();
        comment.setKnowledgeId(knowledgeId);
        comment.setContent(content);
        comment.setCreateBy(loginUser.getUserName());
        comment.setUserName(loginUser.getUserName());
        comment.setUserId(loginUser.getUserId());

        // 先保存拿到ID
        return save(comment);
    }

    @Override
    public boolean addReply(Long knowledgeId,
                         Long rootId,
                         Long parentId,
                         Long replyUserId,
                         String replyUserName,
                         String content,
                         LoginUser loginUser) {

        KnowledgePointComment reply = new KnowledgePointComment();
        reply.setKnowledgeId(knowledgeId);
        reply.setRootId(rootId);
        reply.setParentId(parentId);
        reply.setReplyUserId(replyUserId);
        reply.setContent(content);
        reply.setCreateBy(loginUser.getUserName());
        reply.setUserName(loginUser.getUserName());
        reply.setUserId(loginUser.getUserId());
        reply.setReplyUserName(replyUserName);

        return save(reply);
    }

    @Override
    public ResultUtil<KnowledgeCommentVO> getComment(Long knowledgePointId, Long lastCommentId ,LoginUser loginUser) {
        KnowledgeCommentVO knowledgeCommentVO = new KnowledgeCommentVO();
        List<KnowledgePointComment> list = list(Wrappers.<KnowledgePointComment>lambdaQuery()
                .eq(KnowledgePointComment::getKnowledgeId, knowledgePointId)
                .isNull(KnowledgePointComment::getParentId)
                .lt( lastCommentId!=null, KnowledgePointComment::getId, lastCommentId)
                .orderByDesc(KnowledgePointComment::getCreateTime)
        );

        if(list.isEmpty()){
            knowledgeCommentVO.setHasMore(false);
            return ResultUtil.success(knowledgeCommentVO);
        }

        Set<String> userIds = list.stream().map(item-> String.valueOf(item.getUserId())).collect(Collectors.toSet());
        Map<Long, String> imageInIds = userFeign.getImageInIds(new ArrayList<>(userIds));



        knowledgeCommentVO.setLastCommentId(String.valueOf(list.getLast().getId()));
        knowledgeCommentVO.setLastCommentTime(list.getLast().getCreateTime());

        List<Long> rootIds = list.stream().map(KnowledgePointComment::getId).toList();
        List<CommentCountDTO> commentNumByCommentId = baseMapper.getCommentNumByCommentId(rootIds);
        Map<Long, Integer> collect;
        if(commentNumByCommentId!= null && !commentNumByCommentId.isEmpty()){
            collect = commentNumByCommentId.stream().collect(Collectors.toMap(CommentCountDTO::getRootId, CommentCountDTO::getCount));
        } else {
            collect = Map.of();
        }

        List<KnowledgePointCommentDTO> records = list.stream().map(item -> {
            KnowledgePointCommentDTO dto = item.toDTO();
            if (imageInIds.containsKey(item.getUserId())) {
                dto.setUserHead(imageInIds.get(item.getUserId()));
            }
            if(YesOrNoEnum.YES.getCode().equals(loginUser.getIsUserLogin())){
                dto.setIsBelongUser(loginUser.getUserId().equals(item.getUserId()));
            }else{
                dto.setIsBelongUser(false);
            }
            if(collect.containsKey(item.getId())){
                dto.setReplyNum(collect.get(item.getId()));
            }
            return dto;
        }).toList();
        knowledgeCommentVO.setKnowledgePointCommentDTOList(records);
        knowledgeCommentVO.setHasMore(!records.isEmpty());
        return ResultUtil.success(knowledgeCommentVO);
    }

    /**
     * 使得同一段的回复信息包装成一块
     *
     * @param parentId
     * @param lastReplyId
     * @return
     */
    @Override
    public ResultUtil<KnowledgeReplyVO> getReply(Long parentId, Long lastReplyId,LoginUser loginUser) {
        // 查询直接回复A的评论（分页）

        LambdaQueryWrapper<KnowledgePointComment> wrapper =
                Wrappers.<KnowledgePointComment>lambdaQuery()
                        .eq(KnowledgePointComment::getParentId, parentId)
                        .orderByDesc(KnowledgePointComment::getCreateTime);

        if (lastReplyId != null) {
            wrapper.lt(KnowledgePointComment::getId, lastReplyId);
        }

        wrapper.last("limit " + replyPageSize);

        List<KnowledgePointComment> parentList = list(wrapper);



        if (parentList.isEmpty()) {
            KnowledgeReplyVO empty = new KnowledgeReplyVO();
            empty.setHasMore(false);
            empty.setKnowledgePointReplyDTOList(List.of());
            return ResultUtil.success(empty);
        }

        // 查询该 root 下所有回复

        List<KnowledgePointComment> allReplies =
                list(Wrappers.<KnowledgePointComment>lambdaQuery()
                        .eq(KnowledgePointComment::getRootId, parentId)
                        .orderByAsc(KnowledgePointComment::getCreateTime)
                );

        Map<Long, List<KnowledgePointComment>> replyMap =
                allReplies.stream()
                        .collect(Collectors.groupingBy(
                                KnowledgePointComment::getParentId
                        ));


        List<KnowledgePointComment> result = new ArrayList<>();

        for (KnowledgePointComment parent : parentList) {
            appendReplies(parent, replyMap, result);
        }

        Set<String> ids = result.stream().map(item -> String.valueOf(item.getUserId())).collect(Collectors.toSet());
        ids.addAll(result.stream().map(item -> String.valueOf(item.getReplyUserId())).collect(Collectors.toSet()));
        Map<Long, String> imageInIds = userFeign.getImageInIds(new ArrayList<>(ids));

        List<KnowledgePointReplyDTO> records =
                result.stream()
                        .map(item->{
                            KnowledgePointReplyDTO replyDTO = item.toReplyDTO();
                            if (imageInIds.containsKey(item.getUserId())) {
                                replyDTO.setUserHead(imageInIds.get(item.getUserId()));
                            }
                            if(imageInIds.containsKey(item.getReplyUserId())) {
                                replyDTO.setReplyUserHead(imageInIds.get(item.getReplyUserId()));
                            }

                            if(YesOrNoEnum.YES.getCode().equals(loginUser.getIsUserLogin())){
                                replyDTO.setIsBelongUser(loginUser.getUserId().equals(item.getUserId()));
                            }else{
                                replyDTO.setIsBelongUser(false);
                            }

                            return replyDTO;
                        })
                        .toList();


        KnowledgeReplyVO vo = new KnowledgeReplyVO();

        KnowledgePointComment last = parentList.get(parentList.size() - 1);

        vo.setLastReplyId(String.valueOf(last.getId()));
        vo.setLastReplyTime(last.getCreateTime());
        vo.setHasMore(parentList.size() == replyPageSize);
        vo.setKnowledgePointReplyDTOList(records);

        return ResultUtil.success(vo);
    }

    /**
     * 递归组装数据
     * @param parent
     * @param replyMap
     * @param result
     */
    private void appendReplies(
            KnowledgePointComment parent,
            Map<Long, List<KnowledgePointComment>> replyMap,
            List<KnowledgePointComment> result) {

        result.add(parent);

        List<KnowledgePointComment> children =
                replyMap.getOrDefault(parent.getId(), List.of());

        for (KnowledgePointComment child : children) {
            appendReplies(child, replyMap, result);
        }
    }

    @Transactional
    @Override
    public ResultUtil<String> deleteComment(Long commentId , LoginUser loginUser) {

        KnowledgePointComment comment = getById(commentId);

        if (comment == null) {
            return ResultUtil.fail("此评论不存在");
        }

        if(!Objects.equals(comment.getUserId(),loginUser.getUserId())){
            return ResultUtil.fail("当前用户无权删除");
        }

        // 一级评论
        if (comment.getRootId() == null) {

            remove(Wrappers.<KnowledgePointComment>lambdaQuery()
                    .eq(KnowledgePointComment::getId, commentId)
                    .or()
                    .eq(KnowledgePointComment::getRootId, commentId)
            );

        } else {

            remove(Wrappers.<KnowledgePointComment>lambdaQuery()
                    .eq(KnowledgePointComment::getId, commentId)
                    .or()
                    .eq(KnowledgePointComment::getParentId, commentId)
            );
        }

        return  ResultUtil.success("删除成功");
    }



}
