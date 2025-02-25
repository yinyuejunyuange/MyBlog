package org.oyyj.blogservice.service.impl;

import com.github.jeffreyning.mybatisplus.service.MppServiceImpl;
import org.oyyj.blogservice.mapper.BlogTypeMapper;
import org.oyyj.blogservice.pojo.BlogType;
import org.oyyj.blogservice.service.IBlogTypeService;
import org.springframework.stereotype.Service;

@Service
public class BlogTypeServiceImpl extends MppServiceImpl<BlogTypeMapper, BlogType> implements IBlogTypeService {
}
