package org.oyyj.adminservice.feign;

import jakarta.servlet.http.HttpServletRequest;
import org.oyyj.adminservice.config.FeignRequestConfig;
import org.oyyj.adminservice.dto.PageDTO;
import org.oyyj.adminservice.dto.UserReportForAdminDTO;
import org.oyyj.adminservice.vo.AdminUpdateUserReportVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@FeignClient(value = "UserService",configuration = FeignRequestConfig.class)
public interface UsersFeign {
    @GetMapping("/myBlog/user/getUserNum")
    Long getUserNum();

    @GetMapping("/myBlog/user/getUserInfoList")
    String getUserInfoList(@RequestParam(value = "name",required = false) String name,
                                       @RequestParam(value = "email",required = false) String email,
                                       @RequestParam(value = "startDate",required = false) Date startDate,
                                       @RequestParam(value = "endDate",required = false) Date endDate,
                                       @RequestParam(value = "status",required = false) String status,
                                 @RequestParam(value = "currentPage") Integer currentPage);

    @PutMapping("/myBlog/user/updateUserStatus")
    Map<String,Object> updateUserStatus(@RequestBody Map<String,Object> userUpdateMap);

    @DeleteMapping("/myBlog/user/deleteUser")
    Boolean deleteUser(@RequestParam("userId") Long userId);

    @DeleteMapping("/myBlog/user/deleteUserReport")
    Map<String ,Object> deleteUserReport(@RequestParam("userReportId")String userReportId);

    @PutMapping("/myBlog/user/updateUserReport")
    Map<String,Object> updateUserReport(@RequestBody AdminUpdateUserReportVO adminUpdateUserReportVO);

    @GetMapping("/myBlog/user/getUserReports")
    PageDTO<UserReportForAdminDTO> getUserReports(@RequestParam("currentPage") Integer currentPage,
                                                  @RequestParam(value = "adminName",required = false) String adminName,
                                                  @RequestParam(value = "status",required = false) Integer status);

}
