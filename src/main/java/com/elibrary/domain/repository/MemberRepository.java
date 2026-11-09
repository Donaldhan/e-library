package com.elibrary.domain.repository;

import com.elibrary.domain.model.Member;

import java.util.Optional;

public interface MemberRepository {

    Optional<Member> findById(String id);

    Optional<Member> findByToken(String token);

    Member save(Member member);

    long count();
}
