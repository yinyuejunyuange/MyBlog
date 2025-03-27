package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.CommentReportForAdminDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.pojo.CommentReport;
import org.oyyj.blogservice.vo.AdminUpdateBlogReportVO;
import org.oyyj.blogservice.vo.AdminUpdateCommentReportVO;
import org.oyyj.blogservice.vo.CommentReportVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface ICommentReportService extends IService<CommentReport> {
    Map<String,Object> commentReport(CommentReportVO commentReport);

    PageDTO<CommentReportForAdminDTO>  reportCommentsPage(Integer currentPage,
                                                          String adminName,
                                                          Integer status);
    Map<String,Object> updateCommentReport( AdminUpdateCommentReportVO adminUpdateCommentReportVO);
}
