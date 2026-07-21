package com.skylark.skylarkbiagentbackend.resolution;

import com.skylark.skylarkbiagentbackend.config.EntityResolutionProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityResolutionServiceTest {

    private final EntityResolutionService service = new EntityResolutionService(
            new EntityResolutionProperties(List.of("WOCOMPANY_", "COMPANY"), 0.85, 0.70));

    @Test
    void resolve_exactCodeMatch_despiteDifferentNamespacePrefixes() {
        // The real Phase 1 finding: COMPANY089 (Deal Tracker) vs WOCOMPANY_089 (Work Orders).
        List<ClientIdentity> deals = List.of(new ClientIdentity("COMPANY089", "Naruto", "deals"));
        List<ClientIdentity> workOrders = List.of(new ClientIdentity("WOCOMPANY_089", "Different Alias Name", "workOrders"));

        List<ResolvedEntity> results = service.resolve(deals, workOrders);

        assertThat(results).hasSize(1);
        ResolvedEntity match = results.get(0);
        assertThat(match.confidence()).isEqualTo(MatchConfidence.HIGH);
        assertThat(match.dealClientCode()).isEqualTo("COMPANY089");
        assertThat(match.workOrderClientCode()).isEqualTo("WOCOMPANY_089");
    }

    @Test
    void resolve_fuzzyNameMatch_whenCodesDontAlign() {
        List<ClientIdentity> deals = List.of(new ClientIdentity("COMPANY001", "Skylark Drones Pvt Ltd", "deals"));
        List<ClientIdentity> workOrders = List.of(new ClientIdentity("WOCOMPANY_999", "Skylark Drones Pvt. Ltd", "workOrders"));

        List<ResolvedEntity> results = service.resolve(deals, workOrders);

        assertThat(results).hasSize(1);
        ResolvedEntity match = results.get(0);
        assertThat(match.confidence()).isIn(MatchConfidence.MEDIUM, MatchConfidence.LOW);
        assertThat(match.workOrderClientCode()).isEqualTo("WOCOMPANY_999");
    }

    @Test
    void resolve_noPlausibleMatch_isReportedAsUnmatchedNotDropped() {
        List<ClientIdentity> deals = List.of(new ClientIdentity("COMPANY050", "Aang", "deals"));
        List<ClientIdentity> workOrders = List.of(new ClientIdentity("WOCOMPANY_777", "Completely Unrelated Client", "workOrders"));

        List<ResolvedEntity> results = service.resolve(deals, workOrders);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.confidence() == MatchConfidence.UNMATCHED);
        assertThat(results).anyMatch(r -> "COMPANY050".equals(r.dealClientCode()) && r.workOrderClientCode() == null);
        assertThat(results).anyMatch(r -> "WOCOMPANY_777".equals(r.workOrderClientCode()) && r.dealClientCode() == null);
    }

    @Test
    void resolve_leadingZerosInNumericSuffixDoNotPreventAMatch() {
        List<ClientIdentity> deals = List.of(new ClientIdentity("COMPANY007", "Sasuke", "deals"));
        List<ClientIdentity> workOrders = List.of(new ClientIdentity("WOCOMPANY_7", "Sasuke Alias", "workOrders"));

        List<ResolvedEntity> results = service.resolve(deals, workOrders);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).confidence()).isEqualTo(MatchConfidence.HIGH);
    }
}
