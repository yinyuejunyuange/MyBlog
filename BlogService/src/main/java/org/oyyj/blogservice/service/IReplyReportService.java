package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.CommentReportForAdminDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.dto.ReplyReportForAdminDTO;
import org.oyyj.blogservice.pojo.ReplyReport;
import org.oyyj.blogservice.vo.AdminUpdateCommentReportVO;
import org.oyyj.blogservice.vo.AdminUpdateReplyReportVO;
import org.oyyj.blogservice.vo.ReplyReportVO;

import java.util.Map;

public interface IReplyReportService extends IService<ReplyReport> {

    Map<String,Object> ReplyReport(ReplyReportVO replyReportVO);

    PageDTO<ReplyReportForAdminDTO> reportReplyPage(Integer currentPage,
                                                    String adminName,
                                                    Integer status);

    Map<String,Object> updateReplyReport( AdminUpdateReplyReportVO adminUpdateReplyReportVO);
}
