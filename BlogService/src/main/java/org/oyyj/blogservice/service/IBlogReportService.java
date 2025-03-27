package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.blogservice.dto.BlogReportForAdminDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.pojo.BlogReport;
import org.oyyj.blogservice.vo.AdminUpdateBlogReportVO;
import org.oyyj.blogservice.vo.BlogReportVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface IBlogReportService extends IService<BlogReport> {
    Map<String,Object> reportBlogs(BlogReportVO blogReportVO);

    PageDTO<BlogReportForAdminDTO> reportBlogsPage( Integer currentPage,
                                                    String adminName,
                                                    Integer status);

    Map<String,Object> updateBlogReport( AdminUpdateBlogReportVO adminUpdateBlogReportVO);
}
