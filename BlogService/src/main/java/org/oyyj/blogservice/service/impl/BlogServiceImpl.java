package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.dto.ReadDTO;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.TypeTableMapper;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.BlogType;
import org.oyyj.blogservice.pojo.TypeTable;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.IBlogTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private TypeTableMapper typeTableMapper;

    @Autowired
    private IBlogTypeService blogTypeService;

    @Override
    public void saveBlog(Blog blog) {
        save(blog);
        Long id = blog.getId();

        if(blog.getTypeList() !=null&&!blog.getTypeList().isEmpty()){
            // 相关联的类型
            List<TypeTable> typeTables = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getName, blog.getTypeList()));

            List<BlogType> list = typeTables.stream().map(i -> BlogType.builder().blogId(id).typeId(i.getId()).build()).toList(); // 流式处理
            blogTypeService.saveBatch(list);
        }
    }

    @Override
    public ReadDTO ReadBlog(Long id) {

        // 获取 blog的主要信息
        Blog one = getOne(Wrappers.<Blog>lambdaQuery().eq(Blog::getId, id));

        if(Objects.isNull(one)){
            return null;
        }
        // 获取与其相关的类型type
        List<BlogType> list = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, id));

        // 封装

        ReadDTO readDTO = ReadDTO.builder()
                .id(String.valueOf(id))
                .userId(one.getUserId())
                .title(one.getTitle())
                .Introduce(one.getIntroduce())
                .context(one.getContext())
                .createTime(one.getCreateTime())
                .updateTime(one.getUpdateTime())
                .build();
        if(!Objects.isNull(list)){
            List<String> typeList = list.stream().
                    map(i -> typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery()
                            .eq(TypeTable::getId, i.getTypeId())).getName()).toList();
            readDTO.setTypeList(typeList);
        }

        return readDTO;
    }
}
