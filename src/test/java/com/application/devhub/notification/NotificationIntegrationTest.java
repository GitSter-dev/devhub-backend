package com.application.devhub.notification;

import com.application.devhub.IntegrationTest;
import com.application.devhub.TestUsers;
import com.application.devhub.outbox.OutboxRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "devhub.notifications.fan-out-chunk=2")
class NotificationIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private TestUsers users;

    private NotificationFixtures fixtures;
    private UUID adaId;
    private UUID kenId;
    private String ada;
    private String ken;
    private String linus;
    private String grace;

    @BeforeEach
    void setUp() throws Exception {
        fixtures = new NotificationFixtures(mockMvc, jsonMapper, jdbcTemplate, outboxRelay);
        adaId = users.verified("ada", "Ada");
        kenId = users.verified("ken", "Ken");
        users.verified("linus", "Linus");
        users.verified("grace", "Grace");
        ada = users.bearer("ada");
        ken = users.bearer("ken");
        linus = users.bearer("linus");
        grace = users.bearer("grace");
    }

    @Test
    void likesFromSeveralPeopleCollapseIntoOneRowWithTheLatestFirst() throws Exception {
        UUID post = fixtures.publish(ada, "Shipped the parser");
        fixtures.like(ken, post);
        fixtures.like(linus, post);
        fixtures.like(grace, post);
        fixtures.drain();

        JsonNode items = fixtures.items(ada);
        assertThat(items).hasSize(1);
        JsonNode liked = items.get(0);
        assertThat(liked.get("type").asString()).isEqualTo("POST_LIKED");
        assertThat(liked.get("actorCount").asInt()).isEqualTo(3);
        assertThat(names(liked)).containsExactly("Grace", "Linus", "Ken");
        assertThat(liked.get("subjectId").asString()).isEqualTo(post.toString());
        assertThat(liked.get("preview").asString()).isEqualTo("Shipped the parser");
        assertThat(liked.get("seen").asBoolean()).isFalse();
        assertThat(fixtures.unseen(ada)).isEqualTo(1);
    }

    @Test
    void likingYourOwnPostNotifiesNoOne() throws Exception {
        UUID post = fixtures.publish(ada, "Mine");
        fixtures.like(ada, post);
        fixtures.drain();

        assertThat(fixtures.items(ada)).isEmpty();
    }

    @Test
    void unlikingWithdrawsTheLikerAndAnEmptyGroupDisappears() throws Exception {
        UUID post = fixtures.publish(ada, "Hot take");
        fixtures.like(ken, post);
        fixtures.like(linus, post);
        fixtures.drain();
        fixtures.unlike(ken, post);
        fixtures.drain();

        JsonNode liked = fixtures.items(ada).get(0);
        assertThat(liked.get("actorCount").asInt()).isEqualTo(1);
        assertThat(names(liked)).containsExactly("Linus");

        fixtures.unlike(linus, post);
        fixtures.drain();

        assertThat(fixtures.items(ada)).isEmpty();
        assertThat(fixtures.unseen(ada)).isZero();
    }

    @Test
    void aLikeUndoneBeforeItIsProcessedLeavesNothing() throws Exception {
        UUID post = fixtures.publish(ada, "Toggle me");
        fixtures.like(ken, post);
        fixtures.unlike(ken, post);
        fixtures.drain();

        assertThat(fixtures.items(ada)).isEmpty();
    }

    @Test
    void seeingAGroupClosesItSoLaterActivityStartsANewRow() throws Exception {
        UUID post = fixtures.publish(ada, "Thread");
        fixtures.like(ken, post);
        fixtures.drain();
        String until = fixtures.items(ada).get(0).get("updatedAt").asString();

        fixtures.markSeen(ada, until).andExpect(status().isOk());
        fixtures.like(linus, post);
        fixtures.drain();

        JsonNode items = fixtures.items(ada);
        assertThat(items).hasSize(2);
        assertThat(names(items.get(0))).containsExactly("Linus");
        assertThat(items.get(0).get("seen").asBoolean()).isFalse();
        assertThat(names(items.get(1))).containsExactly("Ken");
        assertThat(items.get(1).get("seen").asBoolean()).isTrue();
        assertThat(fixtures.unseen(ada)).isEqualTo(1);
    }

    @Test
    void markingSeenLeavesActivityNewerThanUntilUnseen() throws Exception {
        UUID post = fixtures.publish(ada, "Thread");
        fixtures.like(ken, post);
        fixtures.drain();
        Instant updatedAt = Instant.parse(fixtures.items(ada).get(0).get("updatedAt").asString());

        fixtures.markSeen(ada, updatedAt.minusSeconds(1).toString()).andExpect(status().isOk());

        assertThat(fixtures.unseen(ada)).isEqualTo(1);
    }

    @Test
    void markingSeenRequiresUntil() throws Exception {
        fixtures.markSeen(ada, "not-a-time").andExpect(status().isBadRequest());
    }

    @Test
    void aReplyNotifiesTheParentAuthorAndPointsAtTheReply() throws Exception {
        UUID post = fixtures.publish(ada, "Question");
        UUID reply = fixtures.reply(ken, post, "Answer");
        fixtures.reply(ada, post, "Replying to myself");
        fixtures.drain();

        JsonNode items = fixtures.items(ada);
        assertThat(items).hasSize(1);
        JsonNode replied = items.get(0);
        assertThat(replied.get("type").asString()).isEqualTo("POST_REPLIED");
        assertThat(replied.get("subjectId").asString()).isEqualTo(post.toString());
        assertThat(replied.get("targetId").asString()).isEqualTo(reply.toString());
        assertThat(replied.get("preview").asString()).isEqualTo("Answer");
    }

    @Test
    void deletingAReplyWithdrawsItsNotification() throws Exception {
        UUID post = fixtures.publish(ada, "Question");
        UUID reply = fixtures.reply(ken, post, "Answer");
        fixtures.drain();

        fixtures.deletePost(ken, reply);

        assertThat(fixtures.items(ada)).isEmpty();
    }

    @Test
    void likesOnADeletedPostAreHidden() throws Exception {
        UUID post = fixtures.publish(ada, "Regret");
        fixtures.like(ken, post);
        fixtures.drain();

        fixtures.deletePost(ada, post);

        assertThat(fixtures.items(ada)).isEmpty();
        assertThat(fixtures.unseen(ada)).isZero();
    }

    @Test
    void followersAreGroupedAndAnUnfollowWithdrawsThem() throws Exception {
        fixtures.follow(ken, adaId);
        fixtures.follow(linus, adaId);
        fixtures.drain();

        JsonNode followed = fixtures.items(ada).get(0);
        assertThat(followed.get("type").asString()).isEqualTo("NEW_FOLLOWER");
        assertThat(names(followed)).containsExactly("Linus", "Ken");

        fixtures.unfollow(linus, adaId);
        fixtures.drain();

        assertThat(names(fixtures.items(ada).get(0))).containsExactly("Ken");
    }

    @Test
    void anOriginalPostReachesEveryFollowerAcrossChunksButRepliesDoNot() throws Exception {
        List<String> followers = IntStream.range(0, 5).mapToObj(index -> "fan" + index).toList();
        for (String follower : followers) {
            users.verified(follower);
            fixtures.follow(users.bearer(follower), adaId);
        }
        fixtures.drain();

        UUID post = fixtures.publish(ada, "New release");
        fixtures.reply(ada, post, "Changelog in the thread");
        fixtures.drain();

        for (String follower : followers) {
            JsonNode items = fixtures.items(users.bearer(follower));
            assertThat(items).hasSize(1);
            assertThat(items.get(0).get("type").asString()).isEqualTo("FOLLOWED_POSTED");
            assertThat(items.get(0).get("targetId").asString()).isEqualTo(post.toString());
            assertThat(items.get(0).get("preview").asString()).isEqualTo("New release");
        }
    }

    @Test
    void postsFromSeveralFolloweesCollapseUntilSeen() throws Exception {
        users.verified("fan");
        String fan = users.bearer("fan");
        fixtures.follow(fan, adaId);
        fixtures.follow(fan, kenId);
        fixtures.publish(ada, "One");
        fixtures.drain();
        UUID latest = fixtures.publish(ken, "Two");
        fixtures.drain();

        JsonNode items = fixtures.items(fan);
        assertThat(items).hasSize(1);
        assertThat(names(items.get(0))).containsExactly("Ken", "Ada");
        assertThat(items.get(0).get("targetId").asString()).isEqualTo(latest.toString());
    }

    @Test
    void aMessageRequestNotifiesTheRecipientUntilAccepted() throws Exception {
        UUID conversation = fixtures.openDirect(ken, adaId);
        fixtures.send(ken, conversation, "Hi Ada");
        fixtures.send(ken, conversation, "Are you there?");
        fixtures.drain();

        JsonNode items = fixtures.items(ada);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("type").asString()).isEqualTo("MESSAGE_REQUEST");
        assertThat(items.get(0).get("targetId").asString()).isEqualTo(conversation.toString());

        fixtures.accept(ada, conversation);
        fixtures.drain();

        assertThat(fixtures.items(ada)).isEmpty();
    }

    @Test
    void messagesBetweenPeopleWhoFollowEachOtherNeverReachTheList() throws Exception {
        fixtures.follow(ada, kenId);
        fixtures.drain();
        UUID conversation = fixtures.openDirect(ken, adaId);
        fixtures.send(ken, conversation, "Hi Ada");
        fixtures.drain();

        assertThat(fixtures.items(ada)).isEmpty();
    }

    @Test
    void theNotificationsListNeedsAToken() throws Exception {
        mockMvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
    }

    private static List<String> names(JsonNode notification) {
        return notification.get("actors").valueStream().map(actor -> actor.get("displayName").asString()).toList();
    }
}
