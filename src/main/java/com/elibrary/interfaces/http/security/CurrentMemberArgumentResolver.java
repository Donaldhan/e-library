package com.elibrary.interfaces.http.security;

import com.elibrary.domain.exception.UnauthorizedException;
import com.elibrary.infrastructure.auth.MemberPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == MemberPrincipal.class
                && parameter.hasParameterAnnotation(CurrentMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        CurrentMember annotation = parameter.getParameterAnnotation(CurrentMember.class);
        Object principal = webRequest.getAttribute(MemberPrincipal.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (principal == null && annotation != null && annotation.required()) {
            throw new UnauthorizedException("A valid bearer token is required for this operation");
        }
        return principal;
    }
}
