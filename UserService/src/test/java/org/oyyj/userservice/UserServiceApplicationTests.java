package org.oyyj.userservice;

import org.junit.jupiter.api.Test;
import org.oyyj.userservice.pojo.LoginUser;
import org.oyyj.userservice.pojo.User;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserServiceApplicationTests {

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

}
