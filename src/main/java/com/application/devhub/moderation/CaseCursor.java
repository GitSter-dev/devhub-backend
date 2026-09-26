package com.application.devhub.moderation;

import com.application.devhub.common.api.ApiException;
import com.application.devhub.moderation.ModerationViews.CaseView;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

record CaseCursor(int severity, int reporterCount, Instant lastReportedAt, UUID id) {

    private static final String SEPARATOR = "|";

    static CaseCursor after(CaseView view) {
        return new CaseCursor(view.severity(), view.reporterCount(), view.lastReportedAt(), view.id());
    }

    String encode() {
        String raw = String.join(SEPARATOR, String.valueOf(severity), String.valueOf(reporterCount),
                lastReportedAt.toString(), id.toString());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    Timestamp lastReportedTimestamp() {
        return Timestamp.from(lastReportedAt);
    }

    static CaseCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
                    .split(Pattern.quote(SEPARATOR), 4);
            return new CaseCursor(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Instant.parse(parts[2]),
                    UUID.fromString(parts[3]));
        } catch (IllegalArgumentException | DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            throw ApiException.badRequest();
        }
    }
}
