package com.elibrary.interfaces.http.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated {@code MemberPrincipal} into a controller method.
 *
 * <p>{@code required = false} is used for endpoints that are public but personalise their response
 * when a token happens to be present (e.g. {@code borrowedByMe} on book details).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {

    boolean required() default true;
}
