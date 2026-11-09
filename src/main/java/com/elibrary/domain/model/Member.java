package com.elibrary.domain.model;

import com.elibrary.domain.model.converter.InstantIsoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * A library member. The {@code token} is the opaque bearer credential used by the demo
 * authentication adapter; it is never exposed through the API.
 */
@Entity
@Table(name = "members")
public class Member {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    private MembershipType membershipType;

    @Column(name = "max_concurrent_loans", nullable = false)
    private int maxConcurrentLoans;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Convert(converter = InstantIsoConverter.class)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Member() {
        // for JPA
    }

    private Member(String id, String name, String email, MembershipType membershipType,
                   int maxConcurrentLoans, String token, Instant now) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.email = requireText(email, "email");
        this.membershipType = Objects.requireNonNull(membershipType, "membershipType");
        if (maxConcurrentLoans < 0) {
            throw new IllegalArgumentException("maxConcurrentLoans must not be negative");
        }
        this.maxConcurrentLoans = maxConcurrentLoans;
        this.token = requireText(token, "token");
        this.createdAt = Objects.requireNonNull(now, "now");
    }

    public static Member create(String id, String name, String email, MembershipType membershipType,
                                int maxConcurrentLoans, String token, Instant now) {
        return new Member(id, name, email, membershipType, maxConcurrentLoans, token, now);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    public int getMaxConcurrentLoans() {
        return maxConcurrentLoans;
    }

    public String getToken() {
        return token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
