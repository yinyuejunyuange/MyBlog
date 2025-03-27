package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.dto.CommentReportForAdminDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.dto.ReplyReportForAdminDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.ReplyReportMapper;
import org.oyyj.blogservice.pojo.CommentReport;
import org.oyyj.blogservice.pojo.Reply;
import org.oyyj.blogservice.pojo.ReplyReport;
import org.oyyj.blogservice.service.IReplyReportService;
import org.oyyj.blogservice.service.IReplyService;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.AdminUpdateReplyReportVO;
import org.oyyj.blogservice.vo.ReplyReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReplyReportServiceImpl extends ServiceImpl<ReplyReportMapper, ReplyReport> implements IReplyReportService {

    @Autowired
    private IReplyService replyService;

    @Autowired
    private UserFeign userFeign;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ReplyReport(ReplyReportVO replyReportVO) {


        ReplyReport one = getOne(Wrappers.<ReplyReport>lambdaQuery()
                .eq(ReplyReport::getReplyId, Long.valueOf(replyReportVO.getReplyId()))
                .eq(ReplyReport::getUserId, Long.valueOf(replyReportVO.getUserId()))
        );
        if(Objects.nonNull(one)){
            return ResultUtil.successMap(null,"您已经举报 管理员正在处理");
        }

        ReplyReport build = ReplyReport.builder()
                .replyId(Long.valueOf(replyReportVO.getReplyId()))
                .replyContent(replyService.getOne(Wrappers.<Reply>lambdaQuery()
                        .eq(Reply::getId, replyReportVO.getReplyId())).getContext())
                .userId(Long.valueOf(replyReportVO.getUserId()))
                .userName(userFeign.getNameInIds(
                        Collections.singletonList(replyReportVO.getUserId())).get(Long.valueOf(replyReportVO.getUserId())))
                .content(replyReportVO.getContent())
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
    public PageDTO<ReplyReportForAdminDTO> reportReplyPage(Integer currentPage, String adminName, Integer status) {
        LambdaQueryWrapper<ReplyReport> lqw=Wrappers.lambdaQuery();

        if(Objects.nonNull(adminName)&&!adminName.isEmpty()){
            lqw.eq(ReplyReport::getFinishName,adminName);
        }

        if(Objects.nonNull(status)){
            lqw.eq(ReplyReport::getStatus,status);
        }


        IPage<ReplyReport> page=new Page<>(currentPage,20);
        List<ReplyReportForAdminDTO> list = list(page,lqw).stream().map(i -> ReplyReportForAdminDTO.builder()
                .id(String.valueOf(i.getId()))
                .replyId(String.valueOf(i.getReplyId()))
                .replyContent(i.getReplyContent())
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

        PageDTO<ReplyReportForAdminDTO> pageDTO = new PageDTO<>();
        pageDTO.setPageSize((int)page.getSize());
        pageDTO.setPageNow(currentPage);
        pageDTO.setPageList(list);
        pageDTO.setTotal((int)page.getTotal());

        return pageDTO;
    }

    @Override
    public Map<String, Object> updateReplyReport(AdminUpdateReplyReportVO adminUpdateReplyReportVO) {
        ReplyReport one = getOne(Wrappers.<ReplyReport>lambdaQuery()
                .eq(ReplyReport::getId, Long.valueOf(adminUpdateReplyReportVO.getReportId()))
                .last("for update") // 添加悲观锁
        );

        if(Objects.isNull(one)){
            return ResultUtil.failMap("没有相应的举报信息");
        }

        boolean update =update(Wrappers.<ReplyReport>lambdaUpdate()
                .eq(ReplyReport::getId, Long.valueOf(adminUpdateReplyReportVO.getReportId()))
                .set(ReplyReport::getFinishId, adminUpdateReplyReportVO.getAdminId())
                .set(ReplyReport::getFinishTime, new Date())
                .set(ReplyReport::getFinishName, adminUpdateReplyReportVO.getAdminName())
                .set(ReplyReport::getStatus, adminUpdateReplyReportVO.getStatus())
        );
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }
}
