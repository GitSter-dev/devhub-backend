package com.application.devhub.profile;

import java.util.List;

public record PersonPage(List<PersonSummary> items, String nextCursor) {
}
