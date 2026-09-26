package com.application.devhub.admin;

import com.application.devhub.admin.AdminUserViews.UserDetail;
import com.application.devhub.admin.AdminUserViews.UserSummary;
import com.application.devhub.common.api.ApiEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

    private final AdminUserQueries adminUserQueries;

    @Override
    @GetMapping
    public ApiEnvelope<List<UserSummary>> search(@RequestParam(defaultValue = "") String q) {
        return ApiEnvelope.ok(adminUserQueries.search(q));
    }

    @Override
    @GetMapping("/{userId}")
    public ApiEnvelope<UserDetail> detail(@PathVariable UUID userId) {
        return ApiEnvelope.ok(adminUserQueries.detail(userId));
    }
}
