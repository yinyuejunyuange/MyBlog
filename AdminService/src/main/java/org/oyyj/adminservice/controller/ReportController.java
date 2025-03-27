package org.oyyj.adminservice.controller;

import org.oyyj.adminservice.dto.*;
import org.oyyj.adminservice.feign.BlogFeign;
import org.oyyj.adminservice.feign.UsersFeign;
import org.oyyj.adminservice.pojo.LoginAdmin;
import org.oyyj.adminservice.util.ResultUtil;
import org.oyyj.adminservice.vo.AdminUpdateBlogReportVO;
import org.oyyj.adminservice.vo.AdminUpdateCommentReportVO;
import org.oyyj.adminservice.vo.AdminUpdateReplyReportVO;
import org.oyyj.adminservice.vo.AdminUpdateUserReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/report")
public class ReportController {

    @Autowired
    private UsersFeign usersFeign;

    @Autowired
    private BlogFeign blogFeign;


    // 用户 举报方面
    /**
     * 分页查询 举报
     */
    @GetMapping("/getUserReport")
    public Map<String,Object> getUserReport(@RequestParam("currentPage") Integer currentPage,
                             @RequestParam(value = "adminName",required = false) String adminName,
                             @RequestParam(value = "status",required = false) Integer status){
        PageDTO<UserReportForAdminDTO> userReports = usersFeign.getUserReports(currentPage, adminName, status);
        return ResultUtil.successMap(userReports,"查询成功");
    }

    /**
     * 修改举报
     */
    @PutMapping("/updateUserReport")
    public Map<String,Object> updateUserReport(@RequestBody AdminUpdateUserReportVO adminUpdateUserReportVO){
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();
        adminUpdateUserReportVO.setAdminName(principal.getAdmin().getName());
        adminUpdateUserReportVO.setAdminId(principal.getAdmin().getId());
        return usersFeign.updateUserReport(adminUpdateUserReportVO);
    }

    /**
     * 删除举报
     */
    @DeleteMapping("/deleteUserReport")
    public Map<String,Object> deleteUserReport(@RequestParam("id") String id){
        return usersFeign.deleteUserReport(id);
    }


    // 评论 举报方面
    /**
     * 分页查询 举报
     */
    @GetMapping("/getCommentReport")
    public Map<String,Object> getCommentReport(@RequestParam("currentPage") Integer currentPage,
                                            @RequestParam(value = "adminName",required = false) String adminName,
                                            @RequestParam(value = "status",required = false) Integer status){
        PageDTO<CommentReportForAdminDTO> commentReports = blogFeign.getCommentReports(currentPage, adminName, status);
        return ResultUtil.successMap(commentReports,"查询成功");
    }

    /**
     * 修改举报
     */
    @PutMapping("/updateCommentReport")
    public Map<String,Object> updateCommentReport(@RequestBody AdminUpdateCommentReportVO adminUpdateCommentReportVO){
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();
        adminUpdateCommentReportVO.setAdminName(principal.getAdmin().getName());
        adminUpdateCommentReportVO.setAdminId(principal.getAdmin().getId());
        return blogFeign.updateCommentReport(adminUpdateCommentReportVO);
    }

    /**
     * 删除举报
     */
    @DeleteMapping("/deleteCommentReport")
    public Map<String,Object> deleteCommentReport(@RequestParam("id") String id){
        return blogFeign.deleteCommentReport(id);
    }

    // 回复 举报方面
    /**
     * 分页查询 举报
     */
    @GetMapping("/getReplyReport")
    public Map<String,Object> getReplyReport(@RequestParam(value = "currentPage") Integer currentPage,
                                               @RequestParam(value = "adminName",required = false) String adminName,
                                               @RequestParam(value = "status",required = false) Integer status){
        PageDTO<ReplyReportForAdminDTO> replyReports = blogFeign.getReplyReports(currentPage, adminName, status);
        return ResultUtil.successMap(replyReports,"查询成功");
    }

    /**
     * 修改举报
     */
    @PutMapping("/updateReplyReport")
    public Map<String,Object> updateReplyReport(@RequestBody AdminUpdateReplyReportVO adminUpdateReplyReportVO){
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();
        adminUpdateReplyReportVO.setAdminName(principal.getAdmin().getName());
        adminUpdateReplyReportVO.setAdminId(principal.getAdmin().getId());
        return blogFeign.updateReplyReport(adminUpdateReplyReportVO);
    }

    /**
     * 删除举报
     */
    @DeleteMapping("/deleteReplyReport")
    public Map<String,Object> deleteReplyReport(@RequestParam("id") String id){
        return blogFeign.deleteReplyReport(id);
    }

    // 博客 举报方面
    /**
     * 分页查询 举报
     */
    @GetMapping("/getBlogReport")
    public Map<String,Object> getBlogReport(@RequestParam("currentPage") Integer currentPage,
                                             @RequestParam(value = "adminName",required = false) String adminName,
                                             @RequestParam(value = "status",required = false) Integer status){
        PageDTO<BlogReportForAdminDTO> blogReports = blogFeign.getBlogReports(currentPage, adminName, status);
        return ResultUtil.successMap(blogReports,"查询成功");
    }

    /**
     * 修改举报
     */
    @PutMapping("/updateBlogReport")
    public Map<String,Object> updateBlogReport(@RequestBody AdminUpdateBlogReportVO adminUpdateBlogReportVO){
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();
        adminUpdateBlogReportVO.setAdminName(principal.getAdmin().getName());
        adminUpdateBlogReportVO.setAdminId(principal.getAdmin().getId());
        return blogFeign.updateBlogReport(adminUpdateBlogReportVO);
    }

    /**
     * 删除举报
     */
    @DeleteMapping("/deleteBlogReport")
    public Map<String,Object> deleteBlogReport(@RequestParam("id") String id){
        return blogFeign.deleteBlogReport(id);
    }

}
