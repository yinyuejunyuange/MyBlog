package org.oyyj.userservice;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.oyyj.userservice.pojo.LoginUser;
import org.oyyj.userservice.pojo.User;
import org.oyyj.userservice.pojo.UserStar;
import org.oyyj.userservice.service.IUserStarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserServiceApplicationTests {

    @Autowired
    private IUserStarService userStarService;

    @Test
    void contextLoads() {
        User build = User.builder()
                .name("oyyj")
                .password("poyyj")
                .build();
        LoginUser loginUser =new LoginUser();
        loginUser.setUser(build);
        System.out.println(loginUser.getUser().getName());
    }

    @Test
    void testRemove(){



        boolean user = userStarService.remove(Wrappers.<UserStar>lambdaQuery()
                .eq(UserStar::getUserId, 1L)
                .eq(UserStar::getBlogId, 1894939800474304514L)
        );
        System.out.println(user);

    }

}
