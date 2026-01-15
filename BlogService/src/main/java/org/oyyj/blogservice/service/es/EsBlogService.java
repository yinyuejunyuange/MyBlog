package org.oyyj.blogservice.service.es;

import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.es.EsBlog;
import org.springframework.stereotype.Service;

public interface EsBlogService {

    EsBlog saveBlog(EsBlog blog);

}
