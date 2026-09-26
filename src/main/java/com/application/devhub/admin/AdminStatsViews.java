package com.application.devhub.admin;

import java.time.LocalDate;
import java.util.List;

public final class AdminStatsViews {

    private AdminStatsViews() {
    }

    public record Totals(long users, long verifiedUsers, long posts, long openCases, long suspended, long banned) {
    }

    public record Day(LocalDate date, long signups, long posts, long reports, long actions) {
    }

    public record Overview(Totals totals, List<Day> daily) {
    }
}
