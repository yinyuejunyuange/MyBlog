package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.dto.BlogReportForAdminDTO;
import org.oyyj.blogservice.dto.CommentReportForAdminDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.CommentReportMapper;
import org.oyyj.blogservice.pojo.BlogReport;
import org.oyyj.blogservice.pojo.Comment;
import org.oyyj.blogservice.pojo.CommentReport;
import org.oyyj.blogservice.service.ICommentReportService;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.AdminUpdateCommentReportVO;
import org.oyyj.blogservice.vo.CommentReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CommentReportServiceImpl extends ServiceImpl<CommentReportMapper, CommentReport> implements ICommentReportService {

    @Autowired
    private ICommentService commentService;

    @Autowired
    private UserFeign userFeign;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> commentReport(CommentReportVO commentReportVO) {

        CommentReport one = getOne(Wrappers.<CommentReport>lambdaQuery()
                .eq(CommentReport::getCommentId, Long.valueOf(commentReportVO.getCommentId()))
                .eq(CommentReport::getUserId, Long.valueOf(commentReportVO.getUserId()))
        );

        if(Objects.nonNull(one)){
            return ResultUtil.successMap(null,"您已经举报 管理员正在处理");
        }

        CommentReport build = CommentReport.builder()
                .commentId(Long.valueOf(commentReportVO.getCommentId()))
                .commentContent(commentService.getOne(Wrappers.<Comment>lambdaQuery()
                        .eq(Comment::getId, commentReportVO.getCommentId())).getContext())
                .userId(Long.valueOf(commentReportVO.getUserId()))
                .userName(userFeign.getNameInIds(
                        Collections.singletonList(commentReportVO.getUserId())).get(Long.valueOf(commentReportVO.getUserId())))
                .content(commentReportVO.getContent())
                .createTime(new Date())
                .status(0)
                .isDelete(0)
                .build();

        boolean save = save(build);
        if(save){
            return ResultUtil.successMap(null,"举报成功");
        }else{
            return ResultUtil.failMap("举报失败");
        }
    }

    @Override
    public PageDTO<CommentReportForAdminDTO> reportCommentsPage(Integer currentPage, String adminName, Integer status) {
        LambdaQueryWrapper<CommentReport> lqw=Wrappers.lambdaQuery();

        if(Objects.nonNull(adminName)&&!adminName.isEmpty()){
            lqw.eq(CommentReport::getFinishName,adminName);
        }

        if(Objects.nonNull(status)){
            lqw.eq(CommentReport::getStatus,status);
        }


        IPage<CommentReport> page=new Page<>(currentPage,20);
        List<CommentReportForAdminDTO> list = list(page,lqw).stream().map(i -> CommentReportForAdminDTO.builder()
                .id(String.valueOf(i.getId()))
                .commentId(String.valueOf(i.getCommentId()))
                .commentContent(i.getCommentContent())
                .userId(String.valueOf(i.getUserId()))
                .userName(i.getUserName())
                .content(i.getContent())
                .createTime(i.getCreateTime())
                .finishTime(i.getFinishTime())
                .finishId(String.valueOf(i.getFinishId()))
                .finishName(i.getFinishName())
                .status(i.getStatus())
                .isDelete(i.getIsDelete())
                .build()).toList();

        PageDTO<CommentReportForAdminDTO> pageDTO = new PageDTO<>();
        pageDTO.setPageSize((int)page.getSize());
        pageDTO.setPageNow(currentPage);
        pageDTO.setPageList(list);
        pageDTO.setTotal((int)page.getTotal());

        return pageDTO;
    }

    @Override
    public Map<String, Object> updateCommentReport(AdminUpdateCommentReportVO adminUpdateCommentReportVO) {
        CommentReport one = getOne(Wrappers.<CommentReport>lambdaQuery()
                .eq(CommentReport::getId, Long.valueOf(adminUpdateCommentReportVO.getCommentId()))
                .last("for update") // 添加悲观锁
        );

        if(Objects.isNull(one)){
            return ResultUtil.failMap("没有相应的举报信息");
        }

        boolean update =update(Wrappers.<CommentReport>lambdaUpdate()
                .eq(CommentReport::getId, Long.valueOf(adminUpdateCommentReportVO.getCommentId()))
                .set(CommentReport::getFinishId, adminUpdateCommentReportVO.getAdminId())
                .set(CommentReport::getFinishTime, new Date())
                .set(CommentReport::getFinishName, adminUpdateCommentReportVO.getAdminName())
                .set(CommentReport::getStatus, adminUpdateCommentReportVO.getStatus())
        );
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }
}
