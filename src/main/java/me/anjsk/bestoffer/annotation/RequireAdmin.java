package me.anjsk.bestoffer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 메서드 위에 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME) // 프로그램 실행 중에도 살아있음
public @interface RequireAdmin {
}