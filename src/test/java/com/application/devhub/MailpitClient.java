package com.application.devhub;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.client.RestClient;

import java.util.List;

public class MailpitClient {

    private final RestClient restClient;

    public MailpitClient(String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public List<MessageSummary> messages() {
        return restClient.get().uri("/api/v1/messages").retrieve().body(MessageList.class).messages();
    }

    public Message message(String id) {
        return restClient.get().uri("/api/v1/message/{id}", id).retrieve().body(Message.class);
    }

    public void clear() {
        restClient.delete().uri("/api/v1/messages").retrieve().toBodilessEntity();
    }

    record MessageList(List<MessageSummary> messages) {
    }

    public record MessageSummary(
            @JsonProperty("ID") String id,
            @JsonProperty("Subject") String subject,
            @JsonProperty("To") List<Recipient> to) {
    }

    public record Recipient(@JsonProperty("Address") String address) {
    }

    public record Message(@JsonProperty("Subject") String subject, @JsonProperty("HTML") String html) {
    }
}
