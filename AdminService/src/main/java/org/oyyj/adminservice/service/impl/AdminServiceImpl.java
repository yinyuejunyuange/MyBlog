package org.oyyj.adminservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.oyyj.adminservice.dto.AddAdminDTO;
import org.oyyj.adminservice.dto.AdminDTO;
import org.oyyj.adminservice.dto.MenuDTO;
import org.oyyj.adminservice.dto.PageDTO;
import org.oyyj.adminservice.mapper.AdminMapper;
import org.oyyj.adminservice.mapper.RoleMapper;
import org.oyyj.adminservice.pojo.*;
import org.oyyj.adminservice.service.IAdminRoleService;
import org.oyyj.adminservice.service.IAdminService;

import org.oyyj.adminservice.service.IRoleService;
import org.oyyj.adminservice.service.ISysMenuService;
import org.oyyj.adminservice.util.RSAUtil;
import org.oyyj.adminservice.util.RedisUtil;
import org.oyyj.adminservice.util.ResultUtil;
import org.oyyj.adminservice.util.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.sasl.AuthenticationException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {

    private static final String ADMIN = "admin";
    private static final String SUPER_ADMIN = "super_admin";

    private final Logger log= LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RoleMapper roleMapper; // 避免 循环依赖

    @Autowired
    private ISysMenuService sysMenuService;

    @Autowired
    private IAdminRoleService adminRoleService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final char[] chars = {
            '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',  'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    @Override
    public JWTAdmin login(String phone, String password,String encode,String uuid) throws AuthenticationException, JsonProcessingException {
        // 通过uuid从redis中判断数据是否正确
        System.out.println(uuid);
        String redisEncode = (String) redisUtil.get(uuid);
        if(!encode.equals(redisEncode)){
            // 验证码不正确
            throw new AuthenticationException("验证码错误");  // 抛出异常 直接交给 异常处理过滤器
        }

        System.out.println(phone+":"+password);

        // username 是正式姓名 所以按照手机号+密码进行登录
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(phone, password));
        if(Objects.isNull(authenticate)){
            throw new AuthenticationException("用户名或密码错误");
        }

        LoginAdmin principal = (LoginAdmin) authenticate.getPrincipal();
        String token = TokenProvider.createToken(principal, "wenb", "admin");
        // 存入redis 设置时间24小时
        redisUtil.set(String.valueOf(principal.getAdmin().getId())+"admin",token,24L, TimeUnit.HOURS);
        return JWTAdmin.builder()
                .id(String.valueOf(principal.getAdmin().getId()))
                .name(principal.getAdmin().getName())
                .imageUrl(principal.getAdmin().getImageUrl())
                .token(token)
                .isValid(true)
                .build();
    }

    @Override
    public List<MenuDTO> getMenuDTO() throws AuthenticationException {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        if(authorities.contains(new SimpleGrantedAuthority(SUPER_ADMIN))){
            Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, SUPER_ADMIN));
            return sysMenuService.adminMenu(role.getId());
        }else if(authorities.contains(new SimpleGrantedAuthority(ADMIN))){
            Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, ADMIN));
            return sysMenuService.adminMenu(role.getId());
        }else{
            log.error("管理员角色存在问题");
            throw new AuthenticationException("角色存在问题");
        }
    }

    @Override
    public Map<String, Object> updateAdmin(String adminId, Integer isFreeze) {

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();

        boolean update = update(Wrappers.<Admin>lambdaUpdate().eq(Admin::getId, Long.valueOf(adminId))
                .set(Admin::getUpdateBy,principal.getAdmin().getId())
                .set(Admin::getUpdateTime,new Date())
                .set(Admin::getIsFreeze, isFreeze));
        if(update){
            return ResultUtil.successMap(null,"修改成功");
        }else{
            return ResultUtil.failMap("修改失败");
        }
    }

    @Override
    public Map<String, Object> addAdmin(AddAdminDTO addAdminDTO) throws Exception {


        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        LoginAdmin principal = (LoginAdmin) authentication.getPrincipal();


        // 查询是否存在相同手机号的 用户
        List<Admin> list = list(Wrappers.<Admin>lambdaQuery().eq(Admin::getPhone, addAdminDTO.getPhone()));
        if(!list.isEmpty()){
            return ResultUtil.failMap("同一个手机不可存在多用户");
        }

        Date date=new Date();

        // 随机生成一个10位数的密码；
        Random random=new Random();
        List<Character> passwordChar=new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            random.nextInt(35);
            passwordChar.add(chars[random.nextInt(35)]);
        }
        // 使用rsa加密后传递给前端
        StringBuilder sb=new StringBuilder();
        for (Character c : passwordChar) {
            sb.append(c);
        }
        String password = sb.toString();

        Admin build = Admin.builder()
                .name(addAdminDTO.getName())
                .phone(addAdminDTO.getPhone())
                .password(passwordEncoder.encode(password))
                .imageUrl("admin.jpg")
                .email(addAdminDTO.getEmail())
                .createTime(date)
                .createBy(principal.getAdmin().getId())
                .updateTime(date)
                .updateBy(principal.getAdmin().getId())
                .isDelete(0)
                .isFreeze(2)
                .build();

        boolean save = save(build);

        if(save){

            // 绑定 管理员类型
            Role one = roleService.getOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, addAdminDTO.getAdminType())
                    .eq(Role::getIsUsing,1));
            if(Objects.isNull(one)){
                log.error("访问错误 管理员类型");
                throw new IllegalCallerException(" 访问错误 管理员类型");
            }

            AdminRole adminRole = new AdminRole();
            adminRole.setAdminId(build.getId());
            adminRole.setRoleId(one.getId());
            adminRole.setIsValid(1);
            boolean isSave = adminRoleService.save(adminRole);
            if(!isSave){
                log.error("新增管理员失败");
                return ResultUtil.failMap("设置失败");
            }

            // 对密码加密并返回给前端
            Map<String,String> result=new HashMap<>();
            Map<String, String> stringStringMap = RSAUtil.genKeyPair();
            String aPrivate = RSAUtil.Encryption(password, stringStringMap.get("public"));

            result.put("keyWord",aPrivate);
            result.put("privateKey",stringStringMap.get("private"));

            log.info("{} 新增管理成功",principal.getAdmin().getName());
            return ResultUtil.successMap(result,"设置成功");
        }else{
            log.error("新增管理员失败");
            return ResultUtil.failMap("设置失败");
        }
    }

    @Override
    public Map<String, Object> getManagers(String name, String phone, String adminType, String createBy, Date startTime, Date endTime, Integer isFreeze, Integer currentPage) {
        try {
            IPage<Admin> page=new Page<>(currentPage,20);

            LambdaQueryWrapper<Admin> lqw=new LambdaQueryWrapper<>();
            if(Objects.nonNull(name)&&!name.isEmpty()){
                lqw.like(Admin::getName,name);
            }

            if(Objects.nonNull(phone)&&!phone.isEmpty()){
                lqw.like(Admin::getPhone,phone);
            }

            if(Objects.nonNull(adminType)&&!adminType.isEmpty()){
                // 查询到所有有效的 adminType对应的id
                Role one = roleService.getOne(Wrappers.<Role>lambdaQuery().eq(Role::getAdminType, adminType)
                        .eq(Role::getIsUsing,1));
                if(Objects.isNull(one)){
                    log.error("访问错误 管理员类型");
                    throw new IllegalCallerException(" 访问错误 管理员类型");
                }
                List<Long> list = adminRoleService.list(Wrappers.<AdminRole>lambdaQuery()
                                .eq(AdminRole::getRoleId, one.getId())
                                .eq(AdminRole::getIsValid,1)
                        ).stream()
                        .map(AdminRole::getAdminId).toList();

                if(list.isEmpty()){

                }else{
                    lqw.in(Admin::getId,list);
                }
            }

            if(Objects.nonNull(createBy)&&!createBy.isEmpty()){
                // 查询 createBy对应的id
                List<Long> list = list(Wrappers.<Admin>lambdaQuery().like(Admin::getName, createBy)).stream()
                        .map(Admin::getId).toList();
                if(list.isEmpty()){

                }else{
                    lqw.in(Admin::getCreateBy,list);
                }
            }


            if(Objects.nonNull(startTime)){
                lqw.ge(Admin::getCreateTime,startTime);
            }

            if(Objects.nonNull(endTime)){
                lqw.le(Admin::getCreateTime,endTime);
            }

            if(Objects.nonNull(isFreeze)){
                lqw.eq(Admin::getIsFreeze,isFreeze);
            }

            Map<Long, String> collect = roleService.list().stream().collect(Collectors.toMap(Role::getId, Role::getAdminType));



            List<AdminDTO> list = list(page, lqw).stream().map(
                    i ->{
                        AdminDTO build = AdminDTO.builder()
                                .id(String.valueOf(i.getId()))
                                .name(i.getName())
                                .imageUrl(i.getImageUrl())
                                .phone(i.getPhone())
                                .email(i.getEmail())
                                .createTime(i.getCreateTime())
                                .updateTime(i.getUpdateTime())
                                .isFreeze(i.getIsFreeze())
                                .build();


                        AdminRole adminRoleServiceOne = adminRoleService.getOne(Wrappers.<AdminRole>lambdaQuery()
                                .eq(AdminRole::getAdminId, i.getId()));
                        if(Objects.nonNull(adminRoleServiceOne)){
                            build.setAdminType(collect.get(adminRoleServiceOne.getRoleId()));
                        }

                        Admin one = getOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId, i.getCreateBy()));
                        if(Objects.nonNull(one)){
                            build.setCreateBy(one.getName());
                        }
                        Admin admin = getOne(Wrappers.<Admin>lambdaQuery().eq(Admin::getId, i.getUpdateBy()));
                        if(Objects.nonNull(admin)){
                            build.setUpdateBy(admin.getName());
                        }

                        return build;
                    }
            ).toList();

            PageDTO<AdminDTO> pageDTO=new PageDTO<>();
            pageDTO.setPageNow(currentPage);
            pageDTO.setPageSize((int)page.getSize());
            pageDTO.setTotal((int)page.getTotal());
            pageDTO.setPageList(list);

            return ResultUtil.successMap(pageDTO,"查询成功");
        } catch (IllegalCallerException e) {
            log.error("查询管理员失败：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
