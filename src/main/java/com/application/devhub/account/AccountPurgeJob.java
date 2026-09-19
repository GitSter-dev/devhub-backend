package com.application.devhub.account;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountPurgeJob {

    private final AccountPurger purger;

    @Scheduled(cron = "${devhub.scheduling.cleanup-cron}")
    void purgeDeletedAccounts() {
        purger.purgeDue();
    }
}
