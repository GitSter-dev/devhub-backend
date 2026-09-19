package com.application.devhub.common.pagination;

import com.application.devhub.common.api.ApiException;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

public record KeysetCursor(Instant at, UUID id) {

    private static final String SEPARATOR = "|";

    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((at + SEPARATOR + id).getBytes(StandardCharsets.UTF_8));
    }

    public Timestamp timestamp() {
        return Timestamp.from(at);
    }

    public static KeysetCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
                    .split(Pattern.quote(SEPARATOR), 2);
            return new KeysetCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            throw ApiException.badRequest();
        }
    }
}
