package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.dto.ReadDTO;
import org.oyyj.blogservice.pojo.Blog;
import org.springframework.data.domain.Page;

import java.util.List;


public interface IBlogService extends IService<Blog> {
    void saveBlog(Blog blog);

    ReadDTO ReadBlog(Long id,String UserInfoKey);

    PageDTO<BlogDTO> getBlogByPage(int current, int pageSizem, String type);
}
