package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Member;
import com.elibrary.domain.repository.MemberRepository;
import com.elibrary.infrastructure.persistence.jpa.MemberJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository jpa;

    public MemberRepositoryAdapter(MemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Member> findById(String id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Member> findByToken(String token) {
        return jpa.findByToken(token);
    }

    @Override
    public Member save(Member member) {
        return jpa.save(member);
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
