package org.oyyj.userservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.userservice.DTO.PageDTO;
import org.oyyj.userservice.DTO.ReportUserDTO;
import org.oyyj.userservice.DTO.UserReportForAdminDTO;
import org.oyyj.userservice.pojo.UserReport;
import org.oyyj.userservice.vo.AdminUpdateUserReportVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface IUserReportService extends IService<UserReport> {
    Map<String,Object> reportUser(ReportUserDTO reportUserDTO);

    PageDTO<UserReportForAdminDTO> reportUserForAdmin(Integer currentPage,
                                                      String adminName,
                                                      Integer status);

    Map<String,Object> updateUserReport(AdminUpdateUserReportVO adminUpdateUserReportVO);
}
