package org.oyyj.studyservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.studyservice.dto.knowledgePoint.KnowledgePointDTO;
import org.oyyj.studyservice.dto.knowledgePointComment.KnowledgePointCommentDTO;
import org.oyyj.studyservice.pojo.KnowledgePointComment;
import org.oyyj.studyservice.vo.knowledgeComment.KnowledgeCommentVO;
import org.oyyj.studyservice.vo.knowledgeComment.KnowledgeReplyVO;

import java.util.List;

public interface KnowledgePointCommentService extends IService<KnowledgePointComment> {

    /**
     * 查询某知识点下所有一级评论
     */
    List<KnowledgePointComment> listRootComments(Long knowledgeId);

    /**
     * 查询某评论下的所有回复
     */
    List<KnowledgePointComment> listReplies(Long rootId);

    /**
     * 添加评论
     * @param knowledgeId
     * @param content
     * @param loginUser
     * @return
     */
    boolean addComment(Long knowledgeId, String content, LoginUser loginUser);

    /**
     * 添加回复
     * @param knowledgeId
     * @param rootId
     * @param parentId
     * @param replyUserId
     * @param replyUserName
     * @param content
     * @param loginUser
     * @return
     */
    boolean addReply(Long knowledgeId,
                     Long rootId,
                     Long parentId,
                     Long replyUserId,
                     String replyUserName,
                     String content,
                     LoginUser loginUser);

    /**
     * 获取对应知识点的回复信息
     * @param knowledgePointId
     * @param lastCommentId
     * @return
     */
    ResultUtil<KnowledgeCommentVO> getComment(Long knowledgePointId ,Long lastCommentId,LoginUser loginUser);

    /**
     * 分页获取 知识点 comment
     *
     * @param knowledgeBaseId
     * @param userName
     * @param replyCommentId
     * @param currentPage
     * @param pageSize
     * @return
     */
    ResultUtil<Page<KnowledgePointCommentDTO>> getCommentForAdmin(Long knowledgeBaseId ,
                                                                  String userName ,
                                                                  Long replyCommentId,
                                                                  Integer currentPage,
                                                                  Integer pageSize
    );

    /**
     * 设置评论可见
     *
     * @param commentId
     * @return
     */
    ResultUtil<String> setCommentVisible(String commentId);

    /**
     * 设置评论不可见
     *
     * @param commentId
     * @return
     */
    ResultUtil<String> setCommentUnVisible(String commentId);


    /**
     * 获取对应知识点回复的评论信息
     *
     * @param patientId
     * @param lastReplyId
     * @return
     */
    ResultUtil<KnowledgeReplyVO> getReply(Long patientId,Long lastReplyId,LoginUser loginUser);

    /**
     * 用户删除评论
     * @param commentId
     * @param loginUser
     * @return
     */
    ResultUtil<String> deleteComment(Long commentId , LoginUser loginUser);

}