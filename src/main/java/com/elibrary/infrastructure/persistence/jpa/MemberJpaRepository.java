package com.elibrary.infrastructure.persistence.jpa;

import com.elibrary.domain.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, String> {

    Optional<Member> findByToken(String token);
}
