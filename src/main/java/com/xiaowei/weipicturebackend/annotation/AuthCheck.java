package com.xiaowei.weipicturebackend.annotation;

import jakarta.validation.groups.Default;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须的角色
     * @return
     */
    String mustRole () default "";
}
