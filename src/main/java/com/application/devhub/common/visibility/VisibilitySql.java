package com.application.devhub.common.visibility;

public final class VisibilitySql {

    private VisibilitySql() {
    }

    public static String notBlocked(String viewerParam, String userIdColumn) {
        return ("NOT EXISTS (SELECT 1 FROM blocks b WHERE (b.blocker_id = :%1$s AND b.blocked_id = %2$s)"
                + " OR (b.blocker_id = %2$s AND b.blocked_id = :%1$s))").formatted(viewerParam, userIdColumn);
    }

    public static String activeAccount(String userAlias) {
        return "%1$s.email_verified_at IS NOT NULL AND %1$s.banned_at IS NULL AND %1$s.deactivated_at IS NULL"
                .formatted(userAlias);
    }

    public static String visibleUser(String viewerParam, String userAlias) {
        return "(" + activeAccount(userAlias) + " AND " + notBlocked(viewerParam, userAlias + ".id") + ")";
    }
}
