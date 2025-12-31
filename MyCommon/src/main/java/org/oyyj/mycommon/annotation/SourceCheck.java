package org.oyyj.mycommon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求来源检查
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SourceCheck {

    /**
     * 允许访问的服务  默认空数组
     * @return
     */
    String[] allowService() default {} ;

}
