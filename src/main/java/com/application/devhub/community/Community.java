package com.application.devhub.community;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "communities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Community extends BaseEntity {

    @Column(nullable = false, updatable = false, length = 30)
    private String slug;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy", nullable = false, length = 20)
    private JoinPolicy joinPolicy;

    @Column(name = "slow_mode_seconds", nullable = false)
    private int slowModeSeconds;

    @Column(name = "member_count", nullable = false, insertable = false, updatable = false)
    private int memberCount;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "removed_at")
    private Instant removedAt;

    @ElementCollection
    @CollectionTable(name = "community_topics", joinColumns = @JoinColumn(name = "community_id"))
    @Column(name = "topic_slug", length = 40)
    private Set<String> topics = new HashSet<>();

    private Community(UUID founderId, String slug, CommunityDetails details) {
        this.createdBy = founderId;
        this.slug = slug;
        apply(details);
    }

    public static Community found(UUID founderId, String slug, CommunityDetails details) {
        return new Community(founderId, slug, details);
    }

    public void update(CommunityDetails details) {
        apply(details);
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    private void apply(CommunityDetails details) {
        this.name = details.name();
        this.description = details.description();
        this.joinPolicy = details.joinPolicy();
        this.topics.clear();
        this.topics.addAll(details.topics());
    }
}
