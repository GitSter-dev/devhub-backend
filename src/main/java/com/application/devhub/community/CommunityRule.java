package com.application.devhub.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "community_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "community_id", nullable = false, updatable = false)
    private UUID communityId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 300)
    private String body;

    private CommunityRule(UUID communityId, int position, String title, String body) {
        this.communityId = communityId;
        this.position = position;
        this.title = title;
        this.body = body;
    }

    public static CommunityRule of(UUID communityId, int position, String title, String body) {
        return new CommunityRule(communityId, position, title, body);
    }
}
