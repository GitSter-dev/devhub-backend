package com.application.devhub.admin;

import com.application.devhub.admin.AdminStatsViews.Overview;
import com.application.devhub.common.api.ApiEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController implements AdminStatsApi {

    private static final int MAX_DAYS = 90;

    private final AdminStatsQuery adminStatsQuery;

    @Override
    @GetMapping
    public ApiEnvelope<Overview> overview(@RequestParam(defaultValue = "30") int days) {
        return ApiEnvelope.ok(adminStatsQuery.overview(Math.clamp(days, 1, MAX_DAYS)));
    }
}
