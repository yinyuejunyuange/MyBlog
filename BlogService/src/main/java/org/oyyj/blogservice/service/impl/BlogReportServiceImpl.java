package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.dto.BlogReportForAdminDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.BlogReportMapper;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.BlogReport;
import org.oyyj.blogservice.service.IBlogReportService;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.util.ResultUtil;
import org.oyyj.blogservice.vo.AdminUpdateBlogReportVO;
import org.oyyj.blogservice.vo.BlogReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BlogReportServiceImpl extends ServiceImpl<BlogReportMapper, BlogReport> implements IBlogReportService {

    @Autowired
    private IBlogService blogService;

    @Autowired
    private UserFeign userFeign;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reportBlogs(BlogReportVO blogReportVO) {

        BlogReport one = getOne(Wrappers.<BlogReport>lambdaQuery()
                .eq(BlogReport::getBlogId, Long.valueOf(blogReportVO.getBlogId()))
                .eq(BlogReport::getUserId, Long.valueOf(blogReportVO.getUserId()))
        );

        if(Objects.nonNull(one)){
            return ResultUtil.successMap(null,"您已经举报 管理员正在处理");
        }


        BlogReport build = BlogReport.builder()
                .blogId(Long.valueOf(blogReportVO.getBlogId()))
                .blogName(blogService.getOne(Wrappers.<Blog>lambdaQuery().
                        eq(Blog::getId, Long.valueOf(blogReportVO.getBlogId()))).getTitle())
                .userId(Long.valueOf(blogReportVO.getUserId()))
                .userName(userFeign.getNameInIds(
                        Collections.singletonList(blogReportVO.getUserId())).get(Long.valueOf(blogReportVO.getUserId())))
                .content(blogReportVO.getContent())
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
    public PageDTO<BlogReportForAdminDTO> reportBlogsPage(Integer currentPage, String adminName, Integer status) {
        LambdaQueryWrapper<BlogReport> lqw=Wrappers.lambdaQuery();

        if(Objects.nonNull(adminName)&&!adminName.isEmpty()){
            lqw.eq(BlogReport::getFinishName,adminName);
        }

        if(Objects.nonNull(status)){
            lqw.eq(BlogReport::getStatus,status);
        }


        IPage<BlogReport> page=new Page<>(currentPage,20);
        List<BlogReportForAdminDTO> list = list(page,lqw).stream().map(i -> BlogReportForAdminDTO.builder()
                .id(String.valueOf(i.getId()))
                .blogId(String.valueOf(i.getBlogId()))
                .blogTitle(i.getBlogName())
                .userId(String.valueOf(i.getUserId()))
                .userName(i.getUserName())
                .content(i.getContent())
                .createTime(i.getCreateTime())
                .finishName(i.getFinishName())
                .finishId(String.valueOf(i.getFinishId()))
                .finishName(i.getFinishName())
                .status(i.getStatus())
                .isDelete(i.getIsDelete())
                .build()).toList();

        PageDTO<BlogReportForAdminDTO> pageDTO = new PageDTO<>();
        pageDTO.setPageSize((int)page.getSize());
        pageDTO.setPageNow(currentPage);
        pageDTO.setPageList(list);
        pageDTO.setTotal((int)page.getTotal());

        return pageDTO;
    }

    @Override
    public Map<String, Object> updateBlogReport(AdminUpdateBlogReportVO adminUpdateBlogReportVO) {

        BlogReport one = getOne(Wrappers.<BlogReport>lambdaQuery()
                .eq(BlogReport::getId, Long.valueOf(adminUpdateBlogReportVO.getReportId()))
                .last("for update") // 添加悲观锁
        );

        if(Objects.isNull(one)){
            return ResultUtil.failMap("没有相应的举报信息");
        }

        boolean update =update(Wrappers.<BlogReport>lambdaUpdate()
                .eq(BlogReport::getId, Long.valueOf(adminUpdateBlogReportVO.getReportId()))
                .set(BlogReport::getFinishId, adminUpdateBlogReportVO.getAdminId())
                .set(BlogReport::getFinishTime, new Date())
                .set(BlogReport::getFinishName, adminUpdateBlogReportVO.getAdminName())
                .set(BlogReport::getStatus, adminUpdateBlogReportVO.getStatus())
        );
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }
}
