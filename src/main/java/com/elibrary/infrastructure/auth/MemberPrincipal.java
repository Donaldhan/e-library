package com.elibrary.infrastructure.auth;

import com.elibrary.domain.model.Member;

/** Authenticated caller, resolved from the bearer token on each request. */
public record MemberPrincipal(String id, String name, String email) {

    public static final String REQUEST_ATTRIBUTE = "elibrary.member";

    public static MemberPrincipal from(Member member) {
        return new MemberPrincipal(member.getId(), member.getName(), member.getEmail());
    }
}
