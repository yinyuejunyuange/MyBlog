package org.oyyj.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.oyyj.mycommonbase.common.auth.LoginUser;
import org.oyyj.mycommonbase.utils.ResultUtil;
import org.oyyj.userservice.dto.PageDTO;
import org.oyyj.userservice.dto.ReportUserDTO;
import org.oyyj.userservice.dto.UserReportForAdminDTO;
import org.oyyj.userservice.mapper.UserReportMapper;
import org.oyyj.userservice.pojo.User;
import org.oyyj.userservice.pojo.UserReport;
import org.oyyj.userservice.service.IUserReportService;
import org.oyyj.userservice.service.IUserService;
import org.oyyj.userservice.vo.AdminUpdateUserReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserReportServiceImpl extends ServiceImpl<UserReportMapper, UserReport> implements IUserReportService {

    @Autowired
    private IUserService userService;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reportUser(ReportUserDTO reportUserDTO, LoginUser loginUser) {
        // 检查是否存在重复举报
        UserReport one = getOne(Wrappers.<UserReport>lambdaQuery()
                .eq(UserReport::getUserId, loginUser.getUserId())
                .eq(UserReport::getUserReportId, Long.valueOf(reportUserDTO.getUserId()))
        );
        if(Objects.nonNull(one)){
            return ResultUtil.successMap(null,"您已经举报 管理员正在处理");
        }

        UserReport build = UserReport.builder()
                .userReportId(Long.parseLong(reportUserDTO.getUserId()))
                .userReportName(userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getId, Long.valueOf(reportUserDTO.getUserId()))).getName())
                .userId(loginUser.getUserId())
                .userName(loginUser.getUserName())
                .content(reportUserDTO.getContent())
                .createTime(new Date())
                .status(0)
                .isDelete(0)
                .build();
        boolean save = save(build);
        if(save){
            return ResultUtil.successMap(null,"举报成功");
        }

        return ResultUtil.failMap("举报失败");
    }

    @Override
    public PageDTO<UserReportForAdminDTO> reportUserForAdmin(Integer currentPage,String adminName,Integer status) {

        LambdaQueryWrapper<UserReport> lqw=Wrappers.lambdaQuery();

        if(Objects.nonNull(adminName)&&!adminName.isEmpty()){
            lqw.eq(UserReport::getFinishName,adminName);
        }

        if(Objects.nonNull(status)){
            lqw.eq(UserReport::getStatus,status);
        }

        IPage<UserReport> page=new Page<>(currentPage,20);
        List<UserReportForAdminDTO> list = list(page,lqw).stream().map(i -> UserReportForAdminDTO.builder()
                .id(String.valueOf(i.getId()))
                .userReportId(String.valueOf(i.getUserReportId()))
                .userReportName(i.getUserReportName())
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

        PageDTO<UserReportForAdminDTO> pageDTO = new PageDTO<>();
        pageDTO.setPageSize((int)page.getSize());
        pageDTO.setPageNow(currentPage);
        pageDTO.setPageList(list);
        pageDTO.setTotal((int)page.getTotal());

        return pageDTO;
    }

    @Override
    public Map<String, Object> updateUserReport(AdminUpdateUserReportVO adminUpdateUserReportVO) {
        UserReport one = getOne(Wrappers.<UserReport>lambdaQuery()
                .eq(UserReport::getId, Long.valueOf(adminUpdateUserReportVO.getReportId()))
                .last("for update") // 添加悲观锁
        );

        if(Objects.isNull(one)){
            return ResultUtil.failMap("没有相应的举报信息");
        }

        boolean update =update(Wrappers.<UserReport>lambdaUpdate()
                .eq(UserReport::getId, Long.valueOf(adminUpdateUserReportVO.getReportId()))
                .set(UserReport::getFinishId, adminUpdateUserReportVO.getAdminId())
                .set(UserReport::getFinishTime, new Date())
                .set(UserReport::getFinishName, adminUpdateUserReportVO.getAdminName())
                .set(UserReport::getStatus, adminUpdateUserReportVO.getStatus())
        );
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }
}
