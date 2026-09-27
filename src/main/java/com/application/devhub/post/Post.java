package com.application.devhub.post;

import com.application.devhub.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(length = 500)
    private String body;

    @Column(columnDefinition = "text")
    private String code;

    @Column(name = "code_language", length = 20)
    private String codeLanguage;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

    @Column(name = "root_id", updatable = false)
    private UUID rootId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "community_id", updatable = false)
    private UUID communityId;

    private Post(UUID authorId, PostContent content, UUID parentId, UUID rootId, UUID communityId) {
        this.authorId = authorId;
        this.body = content.body();
        this.code = content.code();
        this.codeLanguage = content.codeLanguage();
        this.parentId = parentId;
        this.rootId = rootId;
        this.communityId = communityId;
    }

    public static Post original(UUID authorId, PostContent content, UUID communityId) {
        return new Post(authorId, content, null, null, communityId);
    }

    public static Post replyTo(Post parent, UUID authorId, PostContent content) {
        return new Post(authorId, content, parent.getId(), parent.isReply() ? parent.getRootId() : parent.getId(),
                parent.getCommunityId());
    }

    public boolean isReply() {
        return parentId != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAuthoredBy(UUID userId) {
        return authorId.equals(userId);
    }

    public void remove() {
        this.removedAt = Instant.now();
        this.hiddenAt = null;
    }

    public void hide() {
        if (hiddenAt == null && removedAt == null) {
            this.hiddenAt = Instant.now();
        }
    }

    public void restore() {
        this.removedAt = null;
        this.hiddenAt = null;
    }

    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = Instant.now();
            this.body = null;
            this.code = null;
            this.codeLanguage = null;
        }
    }
}
