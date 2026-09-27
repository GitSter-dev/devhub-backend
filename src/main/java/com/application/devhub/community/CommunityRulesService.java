package com.application.devhub.community;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CommunityRulesService {

    private final CommunityAccess access;
    private final CommunityRuleRepository ruleRepository;

    @Transactional
    public void replace(UUID moderatorId, String slug, CommunityRulesRequest request) {
        Community community = access.live(slug);
        access.moderator(community.getId(), moderatorId);
        ruleRepository.deleteAllOf(community.getId());
        List<CommunityRulesRequest.Rule> rules = request.rules();
        ruleRepository.saveAll(IntStream.range(0, rules.size())
                .mapToObj(position -> CommunityRule.of(community.getId(), position, rules.get(position).title(),
                        rules.get(position).body()))
                .toList());
    }
}
