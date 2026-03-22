package org.oyyj.mycommon.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER}) // 注解 在参数上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
@Documented // 生成文档
public @interface RequestUser {

    boolean required() default true; // 默认使用注解必须存在用户信息

}
