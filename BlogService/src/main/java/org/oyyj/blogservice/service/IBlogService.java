package org.oyyj.blogservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.oyyj.blogservice.dto.ReadDTO;
import org.oyyj.blogservice.pojo.Blog;


public interface IBlogService extends IService<Blog> {
    void saveBlog(Blog blog);

    ReadDTO ReadBlog(Long id);
}
