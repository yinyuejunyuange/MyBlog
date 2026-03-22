package org.oyyj.mycommon.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD}) // 注解 在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
@Documented // 生成文档
public @interface RequestRole {

    String[] role();

}
