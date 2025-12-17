package org.oyyj.userservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.userservice.dto.PageDTO;
import org.oyyj.userservice.dto.ReportUserDTO;
import org.oyyj.userservice.dto.UserReportForAdminDTO;
import org.oyyj.userservice.pojo.UserReport;
import org.oyyj.userservice.vo.AdminUpdateUserReportVO;

import java.util.Map;

public interface IUserReportService extends IService<UserReport> {
    Map<String,Object> reportUser(ReportUserDTO reportUserDTO);

    PageDTO<UserReportForAdminDTO> reportUserForAdmin(Integer currentPage,
                                                      String adminName,
                                                      Integer status);

    Map<String,Object> updateUserReport(AdminUpdateUserReportVO adminUpdateUserReportVO);
}
