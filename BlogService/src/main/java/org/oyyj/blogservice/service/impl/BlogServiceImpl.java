package org.oyyj.blogservice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Mapper;
import org.oyyj.blogservice.dto.*;
import org.oyyj.blogservice.feign.UserFeign;
import org.oyyj.blogservice.mapper.BlogMapper;
import org.oyyj.blogservice.mapper.TypeTableMapper;
import org.oyyj.blogservice.pojo.*;
import org.oyyj.blogservice.service.IBlogService;
import org.oyyj.blogservice.service.IBlogTypeService;
import org.oyyj.blogservice.service.ICommentService;
import org.oyyj.blogservice.service.IReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.transform.Result;
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

    @Autowired
    private ICommentService commentService;

    @Autowired
    private IReplyService replyService;


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



    // 获取评论
    @Override
    public List<ReadCommentDTO> getBlogComment(String blogId,String userInfoKey) {
        List<Comment> list = commentService.list(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getBlogId, Long.valueOf(blogId))
        );
        if(list.isEmpty()){
            return null;
        }
        // 获取用户id集合
        List<String> userIds = list.stream().map(i -> {
            Long userId = i.getUserId();
            return String.valueOf(userId);
        }).toList();

        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);
        Map<Long, String> imageInIds = userFeign.getImageInIds(userIds);

        // 通过list 获取回复的集合
        List<Long> commentIds = list.stream().map(Comment::getId).toList();

        List<Reply> replyList=replyService.list(Wrappers.<Reply>lambdaQuery()
                .in(Reply::getCommentId,commentIds)
        );
        List<String> replyUserIds = replyList.stream().map(i -> {
            Long userid = i.getUserId();
            return String.valueOf(userid);
        }).toList();

        // 获得评论中的用户名和用户头像
        Map<Long, String> replyNameInIds = userFeign.getNameInIds(replyUserIds);
        Map<Long, String> replyImageInIds = userFeign.getImageInIds(replyUserIds);

        if(!Objects.isNull(userInfoKey)){
            List<ReadCommentDTO> readCommentDTOS = list.stream().map(i -> ReadCommentDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(nameInIds.get(i.getUserId()))
                    .UserImage("http://localhost:8080/myBlog/user/getHead/"+imageInIds.get(i.getUserId()))
                    .context(i.getContext())
                    .replyList(
                            replyService.list(Wrappers.<Reply>lambdaQuery()
                                            .eq(Reply::getCommentId, i.getId()))
                                    .stream()
                                    .map(x -> ReadReplyDTO.builder()
                                            .id(String.valueOf(x.getId()))
                                            .userId(String.valueOf(x.getUserId()))
                                            .userName(replyNameInIds.get(x.getUserId()))
                                            .userImage("http://localhost:8080/myBlog/user/getHead/"+replyImageInIds.get(x.getUserId()))
                                            .context(x.getContext())
                                            .updateTime(x.getUpdateTime())
                                            .kudos(String.valueOf(x.getKudos()))
                                            .isUserKudos(userFeign.getUserKudosReply(String.valueOf(x.getId()),userInfoKey))
                                            .build()).toList())
                    .updateTime(i.getUpdateTime())
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(userFeign.getUserKudosComment(String.valueOf(i.getId()),userInfoKey))
                    .build()
            ).toList();

            System.out.println("查询成功:"+readCommentDTOS);
            return readCommentDTOS;
        }else{
            List<ReadCommentDTO> readCommentDTOS = list.stream().map(i -> ReadCommentDTO.builder()
                    .id(String.valueOf(i.getId()))
                    .userId(String.valueOf(i.getUserId()))
                    .userName(nameInIds.get(i.getUserId()))
                    .UserImage("http://localhost:8080/myBlog/user/getHead/"+imageInIds.get(i.getUserId()))
                    .context(i.getContext())
                    .replyList(
                            replyService.list(Wrappers.<Reply>lambdaQuery()
                                            .eq(Reply::getCommentId, i.getId()))
                                    .stream()
                                    .map(x -> ReadReplyDTO.builder()
                                            .id(String.valueOf(x.getId()))
                                            .userId(String.valueOf(x.getUserId()))
                                            .userName(replyNameInIds.get(x.getUserId()))
                                            .userImage("http://localhost:8080/myBlog/user/getHead/"+replyImageInIds.get(x.getUserId()))
                                            .context(x.getContext())
                                            .updateTime(x.getUpdateTime())
                                            .kudos(String.valueOf(x.getKudos()))
                                            .isUserKudos(false)
                                            .build()).toList())
                    .updateTime(i.getUpdateTime())
                    .kudos(String.valueOf(i.getKudos()))
                    .isUserKudos(false)
                    .build()
            ).toList();

            System.out.println("查询成功:"+readCommentDTOS);
            return readCommentDTOS;
        }
    }



    // 评论点赞数加一或者减一
    @Override
    @Transactional
    public Boolean changeCommentKudos(Long commentId, Byte bytes) {
        Comment forUpdate = commentService.getOne(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getId, commentId)
                .last("for update")
        );
        if(bytes==1){
            // 1代表增加点赞数
            boolean update = commentService.update(Wrappers.<Comment>lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getKudos, forUpdate.getKudos() + 1)
            );
            System.out.println(update);
            return update;
        }else if(bytes==2){
            boolean update = commentService.update(Wrappers.<Comment>lambdaUpdate()
                    .eq(Comment::getId, commentId)
                    .set(Comment::getKudos, forUpdate.getKudos() -1)
            );
            System.out.println(update);
            return update;
        }else{
            log.error("改变类型错误");
            return null;
        }
    }

    // 回复点赞数加一或者减一
    @Override
    public Boolean changeReplyKudos(Long replyId, Byte bytes) {
        Reply forUpdate = replyService.getOne(Wrappers.<Reply>lambdaQuery()
                .eq(Reply::getId, replyId)
                .last("for update") // 悲观锁
        );
        if(bytes==1){
            // 1代表增加点赞数
            boolean update = replyService.update(Wrappers.<Reply>lambdaUpdate()
                    .eq(Reply::getId, replyId)
                    .set(Reply::getKudos, forUpdate.getKudos() + 1));
            System.out.println(update);
            return update;
        }else if(bytes==2){
            boolean update = replyService.update(Wrappers.<Reply>lambdaUpdate()
                    .eq(Reply::getId, replyId)
                    .set(Reply::getKudos, forUpdate.getKudos() - 1));
            System.out.println(update);
            return update;
        }else{
            log.error("改变类型错误");
            return null;
        }

    }

    /**
     * 获取用户的博客原创和所有的访问数数
     * @param userId 用户id
     * @return 数量集合 1是原创数 2 是访问数 3 总访问数
     */
    @Override
    public List<Long> getUserBlogNum(Long userId){
        List<Blog> list = list(Wrappers.<Blog>lambdaQuery()
                .eq(Blog::getUserId, userId));

        if(list.isEmpty()){
            return null;
        }
        List<Long> resultList=new ArrayList<>();
        long sumKudos = list.stream().mapToLong(Blog::getKudos).sum(); // 总点赞数
        long sumWatch = list.stream().mapToLong(Blog::getWatch).sum();
        resultList.add((long) list.size());
        resultList.add(sumWatch);
        resultList.add(sumKudos);

        return resultList;


    }

    @Override
    public PageDTO<BlogDTO> getBlogByName(int current, int pageSize, String blogName) {



        if(Objects.isNull(blogName)){
            blogName=" ";
        }
        IPage<Blog> page=new Page<>(current,pageSize);
        List<Blog> list = list(page, Wrappers.<Blog>lambdaQuery().like(Blog::getTitle, blogName));
        List<Long> userIdList = list.stream().map(Blog::getUserId).toList();
        List<String> userIds = userIdList.stream().map(String::valueOf).toList();

        // 获取用户信息
        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);
        List<BlogDTO> blogDTOS = list.stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(nameInIds.get(i.getUserId()))
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                        .stream().map(j -> {
                            TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                            return typeTables.getName();
                        }).toList())
                .kudos(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .watch(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .build()
        ).toList();

        PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
        blogDTOPageDTO.setPageList(blogDTOS);
        blogDTOPageDTO.setPageSize(pageSize);
        blogDTOPageDTO.setPageNow(current);
        blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
        return blogDTOPageDTO;

    }

    @Override
    public PageDTO<BlogDTO> getBlogByUserId(int current, int pageSize, Long userId) {

        if(Objects.isNull(userId)){
            return null;
        }
        IPage<Blog> page=new Page<>(current,pageSize);
        List<Blog> list = list(page, Wrappers.<Blog>lambdaQuery().eq(Blog::getUserId, userId));
        List<Long> userIdList = list.stream().map(Blog::getUserId).toList();
        List<String> userIds = userIdList.stream().map(String::valueOf).toList();

        // 获取用户信息
        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);
        List<BlogDTO> blogDTOS = list.stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(nameInIds.get(i.getUserId()))
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                        .stream().map(j -> {
                            TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                            return typeTables.getName();
                        }).toList())
                .kudos(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .watch(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .build()
        ).toList();

        PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
        blogDTOPageDTO.setPageList(blogDTOS);
        blogDTOPageDTO.setPageSize(pageSize);
        blogDTOPageDTO.setPageNow(current);
        blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
        return blogDTOPageDTO;
    }

    @Override
    public PageDTO<BlogDTO> getBlogByTypeList(int current, int pageSize, List<String> typeList) {
        if(Objects.isNull(typeList)){
            return null;
        }
        IPage<Blog> page=new Page<>(current,pageSize);

        List<TypeTable> typeTableList = typeTableMapper.selectList(Wrappers.<TypeTable>lambdaQuery().in(TypeTable::getName, typeList));
        if(typeTableList.isEmpty()){
            return null;
        }
        List<Long> typeIds = typeTableList.stream().map(TypeTable::getId).toList();


        List<BlogType> blogTypeList = blogTypeService.list(Wrappers.<BlogType>lambdaQuery().in(BlogType::getTypeId, typeIds));
        List<Long> blogIds = blogTypeList.stream().map(BlogType::getBlogId).toList();
        if(blogIds.isEmpty()){
            return null;
        }
        List<Blog> list = list(page, Wrappers.<Blog>lambdaQuery().in(Blog::getId, blogIds));
        List<Long> userIdList = list.stream().map(Blog::getUserId).toList();
        List<String> userIds = userIdList.stream().map(String::valueOf).toList();

        // 获取用户信息
        Map<Long, String> nameInIds = userFeign.getNameInIds(userIds);
        List<BlogDTO> blogDTOS = list.stream().map(i -> BlogDTO.builder()
                .id(String.valueOf(i.getId()))
                .title(i.getTitle())
                .context(i.getContext())
                .userId(String.valueOf(i.getUserId()))
                .userName(nameInIds.get(i.getUserId()))
                .introduce(i.getIntroduce())
                .createTime(i.getCreateTime())
                .updateTime(i.getUpdateTime())
                .status(i.getStatus())
                .typeList(blogTypeService.list(Wrappers.<BlogType>lambdaQuery().eq(BlogType::getBlogId, i.getId()))
                        .stream().map(j -> {
                            TypeTable typeTables = typeTableMapper.selectOne(Wrappers.<TypeTable>lambdaQuery().eq(TypeTable::getId, j.getTypeId()));
                            return typeTables.getName();
                        }).toList())
                .kudos(String.valueOf(i.getKudos()))
                .star(String.valueOf(i.getStar()))
                .watch(String.valueOf(i.getWatch()))
                .commentNum(String.valueOf(i.getCommentNum()))
                .build()
        ).toList();

        PageDTO<BlogDTO> blogDTOPageDTO = new PageDTO<>();
        blogDTOPageDTO.setPageList(blogDTOS);
        blogDTOPageDTO.setPageSize(pageSize);
        blogDTOPageDTO.setPageNow(current);
        blogDTOPageDTO.setTotal(Integer.valueOf(String.valueOf(page.getTotal())));
        return blogDTOPageDTO;
    }


}
