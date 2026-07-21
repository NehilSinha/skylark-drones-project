package com.skylark.skylarkbiagentbackend.resolution;

import com.skylark.skylarkbiagentbackend.config.EntityResolutionProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cross-board client identity resolution, per ADR-001 "Entity Resolution
 * Strategy": exact normalized-code match first, fuzzy deal-name match as a
 * fallback, unmatched records always reported rather than dropped.
 *
 * <p>This implementation covers tiers 1 (client code) and 3 (fuzzy name) of the
 * ADR's three-tier chain. Tier 2 (owner+sector shortlist) needs normalized fields
 * from both boards and is deferred until {@code WorkOrderNormalizer} exists — see
 * PHASE-4-DESIGN.md implementation notes. It narrows candidates before the fuzzy
 * fallback; omitting it for now only means the fuzzy match considers a wider
 * candidate set than strictly necessary, not that matches are incorrect.
 */
@Component
public class EntityResolutionService {

    private final EntityResolutionProperties properties;

    public EntityResolutionService(EntityResolutionProperties properties) {
        this.properties = properties;
    }

    public List<ResolvedEntity> resolve(List<ClientIdentity> dealSideClients, List<ClientIdentity> workOrderSideClients) {
        List<ResolvedEntity> results = new ArrayList<>();
        Set<ClientIdentity> matchedWorkOrderSide = new LinkedHashSet<>();

        for (ClientIdentity deal : dealSideClients) {
            String normalizedDealKey = normalizeCode(deal.code());

            ClientIdentity exactMatch = findExactCodeMatch(normalizedDealKey, workOrderSideClients, matchedWorkOrderSide);
            if (exactMatch != null) {
                matchedWorkOrderSide.add(exactMatch);
                results.add(new ResolvedEntity(normalizedDealKey, deal.code(), exactMatch.code(), MatchConfidence.HIGH));
                continue;
            }

            FuzzyMatch fuzzy = findBestFuzzyMatch(deal, workOrderSideClients, matchedWorkOrderSide);
            if (fuzzy != null) {
                matchedWorkOrderSide.add(fuzzy.candidate());
                String key = normalizedDealKey != null ? normalizedDealKey : normalizeName(deal.name());
                results.add(new ResolvedEntity(key, deal.code(), fuzzy.candidate().code(), fuzzy.confidence()));
            } else {
                results.add(new ResolvedEntity(normalizedDealKey, deal.code(), null, MatchConfidence.UNMATCHED));
            }
        }

        for (ClientIdentity workOrder : workOrderSideClients) {
            if (!matchedWorkOrderSide.contains(workOrder)) {
                results.add(new ResolvedEntity(normalizeCode(workOrder.code()), null, workOrder.code(), MatchConfidence.UNMATCHED));
            }
        }

        return results;
    }

    private ClientIdentity findExactCodeMatch(String normalizedDealKey, List<ClientIdentity> candidates, Set<ClientIdentity> alreadyMatched) {
        if (normalizedDealKey == null) {
            return null;
        }
        for (ClientIdentity candidate : candidates) {
            if (alreadyMatched.contains(candidate)) {
                continue;
            }
            if (normalizedDealKey.equals(normalizeCode(candidate.code()))) {
                return candidate;
            }
        }
        return null;
    }

    private FuzzyMatch findBestFuzzyMatch(ClientIdentity deal, List<ClientIdentity> candidates, Set<ClientIdentity> alreadyMatched) {
        String normalizedDealName = normalizeName(deal.name());
        ClientIdentity best = null;
        double bestScore = 0;

        for (ClientIdentity candidate : candidates) {
            if (alreadyMatched.contains(candidate)) {
                continue;
            }
            double score = JaroWinklerSimilarity.similarity(normalizedDealName, normalizeName(candidate.name()));
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null || bestScore < properties.fuzzyPossibleThreshold()) {
            return null;
        }
        MatchConfidence confidence = bestScore >= properties.fuzzyAutoAcceptThreshold() ? MatchConfidence.MEDIUM : MatchConfidence.LOW;
        return new FuzzyMatch(best, confidence);
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String upper = code.trim().toUpperCase(Locale.ROOT);
        for (String prefix : properties.clientCodePrefixesToStrip()) {
            String upperPrefix = prefix.toUpperCase(Locale.ROOT);
            if (upper.startsWith(upperPrefix)) {
                upper = upper.substring(upperPrefix.length());
                break;
            }
        }
        return upper.replaceFirst("^0+(?=\\d)", "");
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private record FuzzyMatch(ClientIdentity candidate, MatchConfidence confidence) {
    }
}
