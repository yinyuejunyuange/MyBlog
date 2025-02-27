package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.oyyj.blogservice.dto.BlogDTO;
import org.oyyj.blogservice.dto.PageDTO;
import org.oyyj.blogservice.dto.ReadDTO;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.TypeTableMapper;
import org.oyyj.blogservice.pojo.Blog;
import org.oyyj.blogservice.pojo.BlogType;
import org.oyyj.blogservice.pojo.TypeTable;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.IBlogTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private TypeTableMapper typeTableMapper;

    @Autowired
    private IBlogTypeService blogTypeService;

    @Autowired
    private UserFeign userFeign;


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
    @Transactional(isolation=Isolation.READ_COMMITTED)  // 保证数据库操作的原子性  设置为 读已提交  避免使用 REPEATABLE READ 导致死循环
    public ReadDTO ReadBlog(Long id,String userInfoKey) {

        // 获取 blog的主要信息
        Blog one = getOne(Wrappers.<Blog>lambdaQuery().eq(Blog::getId, id));

        if(Objects.isNull(one)){
            return null;
        }

        // 每看一次 博客的阅读数量就加一
        boolean update = update(Wrappers.<Blog>lambdaUpdate()
                .eq(Blog::getId, one.getId())
                .eq(Blog::getWatch, one.getWatch()) // 乐观锁 处理高并发问题
                .set(Blog::getWatch, one.getWatch() + 1));
        if(!update){
            // 出现 问题 自旋
            return ReadBlog(id,userInfoKey);
        }

        // 获取与其相关的类型type
        List<BlogType> list = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, id));

        // 封装
        Map<Long, String> userNameMap = userFeign.getUserNameMap();
        ReadDTO readDTO = ReadDTO.builder()
                .id(String.valueOf(id))
                .userId(String.valueOf(one.getUserId()))
                .userName(userNameMap.get(one.getUserId()))
                .title(one.getTitle())
                .Introduce(one.getIntroduce())
                .context(one.getContext())
                .createTime(one.getCreateTime())
                .updateTime(one.getUpdateTime())
                .star(String.valueOf(one.getStar()))
                .commentNum(String.valueOf(one.getCommentNum()))
                .kudos(String.valueOf(one.getKudos()))
                .watch(String.valueOf(one.getWatch()))
                .build();
        if(!Objects.isNull(list)){
            List<String> typeList = list.stream().
                    map(i -> typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery()
                            .eq(TypeTable::getId, i.getTypeId())).getName()).toList();
            readDTO.setTypeList(typeList);
        }

        if(userInfoKey!=null&&!userInfoKey.isEmpty()){
            System.out.println("userInfoKey:"+userInfoKey);
            Boolean userKudos = userFeign.isUserKudos(id,userInfoKey);
            if(userKudos){
                readDTO.setIsUserKudos(userKudos);
            }
            if(userFeign.isUserStar(id,userInfoKey)){
                readDTO.setIsUserStar(true);
            }
        }

        return readDTO;
    }

    @Override
    public PageDTO<BlogDTO> getBlogByPage(int current, int pageSize, String type) {
        IPage<Blog> page = new Page<>(current,pageSize);
        Map<Long, String> userNameMap = userFeign.getUserNameMap();
        if(type==null||type.isEmpty()){
            List<BlogDTO> list = list(page).stream().map(i -> BlogDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .title(i.getTitle())
                    .userId(String.valueOf(i.getUserId()))
                    .userName(userNameMap.get(i.getUserId()))
                    .introduce(i.getIntroduce())
                    .createTime(i.getCreateTime())
                    .updateTime(i.getUpdateTime())
                    .status(i.getStatus())
                    .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                            .stream().map(j -> {
                                TypeTable typeTable = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                                return typeTable.getName();
                            }).toList())
                    .star(String.valueOf(i.getStar()))
                    .kudos(String.valueOf(i.getKudos()))
                    .watch(String.valueOf(i.getWatch()))
                    .commentNum(String.valueOf(i.getCommentNum()))
                    .build()
            ).toList();

            PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
            blogDTOPageDTO.setPageList(list);
            blogDTOPageDTO.setPageSize(pageSize);
            blogDTOPageDTO.setPageNow(current);
            blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
            return blogDTOPageDTO;

        }else{
            TypeTable typeTable = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getName, type));
            List<Long> list = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getTypeId, typeTable.getId()))
                    .stream().map(BlogType::getBlogId).toList();
            if(!list.isEmpty()){
                List<BlogDTO> list2 = list(page, Wrappers.<Blog>lambdaQuery().in(Blog::getId, list)).stream().map(i -> BlogDTO.builder()
                        .id(String.valueOf(i.getId()))
                        .title(i.getTitle())
                      // 暂时不传递 文章内容
                        .userId(String.valueOf(i.getUserId()))
                        .userName(userNameMap.get(i.getUserId()))
                        .introduce(i.getIntroduce())
                        .createTime(i.getCreateTime())
                        .updateTime(i.getUpdateTime())
                        .status(i.getStatus())
                        .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                                .stream().map(j -> {
                                    TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                                    return typeTables.getName();
                                }).toList())
                        .star(String.valueOf(i.getStar()))
                        .kudos(String.valueOf(i.getKudos()))
                        .watch(String.valueOf(i.getWatch()))
                        .commentNum(String.valueOf(i.getCommentNum()))
                        .build()
                ).toList();
                PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
                blogDTOPageDTO.setPageList(list2);
                blogDTOPageDTO.setPageSize(pageSize);
                blogDTOPageDTO.setPageNow(current);
                blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
                return blogDTOPageDTO;
            }else{
                PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
                blogDTOPageDTO.setPageList(null);
                blogDTOPageDTO.setPageSize(pageSize);
                blogDTOPageDTO.setPageNow(current);
                blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
                return blogDTOPageDTO;
            }
        }
    }
}
