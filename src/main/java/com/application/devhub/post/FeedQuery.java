package com.application.devhub.post;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.common.pagination.KeysetCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeedQuery {

    private static final String LIVE_TOP_LEVEL = "p.parent_id IS NULL AND p.deleted_at IS NULL";
    private static final String FOLLOWED = "EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = :viewer AND f.followee_id = p.author_id)";
    private static final String JOINED = "p.community_id IN (SELECT m.community_id FROM community_members m WHERE m.user_id = :viewer)";
    private static final String FOLLOWING_TIER = LIVE_TOP_LEVEL + " AND (p.author_id = :viewer OR " + FOLLOWED + " OR " + JOINED + ")";
    private static final String INTERESTS_TIER = LIVE_TOP_LEVEL + " AND p.author_id <> :viewer AND NOT " + FOLLOWED
            + " AND (p.community_id IS NULL OR NOT " + JOINED + ") " + """
             AND EXISTS (SELECT 1 FROM user_topics mine
                         JOIN user_topics theirs ON theirs.topic_slug = mine.topic_slug
                         WHERE mine.user_id = :viewer AND theirs.user_id = p.author_id)""";

    private final PostViews postViews;

    @Transactional(readOnly = true)
    public PostPage feed(UUID viewerId, String cursor) {
        FeedCursor position = FeedCursor.decode(cursor);
        List<PostView> items = new ArrayList<>();
        if (position.tier() == Tier.FOLLOWING) {
            List<PostView> following = postViews.slice(viewerId, FOLLOWING_TIER, Map.of(), position.keyset(), false,
                    PostViews.PAGE_SIZE + 1);
            if (following.size() > PostViews.PAGE_SIZE) {
                List<PostView> page = following.subList(0, PostViews.PAGE_SIZE);
                return new PostPage(page, new FeedCursor(Tier.FOLLOWING, PostViews.cursorAfter(page.getLast())).encode());
            }
            items.addAll(following);
            position = new FeedCursor(Tier.INTERESTS, null);
        }
        int remaining = PostViews.PAGE_SIZE - items.size();
        List<PostView> interests = postViews.slice(viewerId, INTERESTS_TIER, Map.of(), position.keyset(), false,
                remaining + 1);
        if (interests.size() <= remaining) {
            items.addAll(interests);
            return new PostPage(items, null);
        }
        items.addAll(interests.subList(0, remaining));
        return new PostPage(items, new FeedCursor(Tier.INTERESTS, PostViews.cursorAfter(items.getLast())).encode());
    }

    private enum Tier {
        FOLLOWING,
        INTERESTS
    }

    private record FeedCursor(Tier tier, KeysetCursor keyset) {

        private static final String SEPARATOR = ".";

        String encode() {
            return tier.ordinal() + SEPARATOR + (keyset == null ? "" : keyset.encode());
        }

        static FeedCursor decode(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return new FeedCursor(Tier.FOLLOWING, null);
            }
            int separator = cursor.indexOf(SEPARATOR);
            if (separator < 0) {
                throw ApiException.badRequest();
            }
            String tier = cursor.substring(0, separator);
            if (!tier.equals("0") && !tier.equals("1")) {
                throw ApiException.badRequest();
            }
            return new FeedCursor(Tier.values()[Integer.parseInt(tier)], KeysetCursor.decode(cursor.substring(separator + 1)));
        }
    }
}
