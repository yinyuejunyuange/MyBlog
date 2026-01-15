package org.oyyj.blogservice.service.es.impl;

import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.es.EsBlog;
import org.oyyj.blogservice.repository.EsBlogRepository;
import org.oyyj.blogservice.service.es.EsBlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EsBlogServiceImpl implements EsBlogService {

    @Autowired
    private EsBlogRepository esBlogRepository;


    @Override
    public EsBlog saveBlog(EsBlog blog) {
        return esBlogRepository.save(blog);
    }
}
