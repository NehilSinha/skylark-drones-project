package com.skylark.skylarkbiagentbackend.normalizer;

import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayColumnValueDto;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.config.DealColumnMapping;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DealNormalizerTest {

    private static final DealColumnMapping MAPPING = new DealColumnMapping(
            "owner_code", "client_code", "status", "close_date", "probability",
            "deal_value", "tentative_close_date", "stage", "product", "sector", "created_date");

    private final DealNormalizer normalizer = new DealNormalizer();

    @Test
    void normalize_fullyPopulatedRow() {
        MondayItemDto item = dealItem("Naruto", List.of(
                col("owner_code", "OWNER_001"),
                col("client_code", "COMPANY089"),
                col("status", "Open"),
                col("probability", "High"),
                col("deal_value", "489360"),
                col("tentative_close_date", "2026-02-26"),
                col("stage", "B. Sales Qualified Leads"),
                col("sector", "Mining"),
                col("created_date", "2025-12-26")
        ));

        NormalizedDeal deal = normalizer.normalize(item, MAPPING);

        assertThat(deal.dealName()).isEqualTo("Naruto");
        assertThat(deal.ownerCode()).isEqualTo("OWNER_001");
        assertThat(deal.clientCode()).isEqualTo("COMPANY089");
        assertThat(deal.status()).isEqualTo(DealStatus.OPEN);
        assertThat(deal.closureProbability()).isEqualTo(ClosureProbability.HIGH);
        assertThat(deal.dealValue()).isEqualTo(new BigDecimal("489360"));
        assertThat(deal.tentativeCloseDate()).isEqualTo(LocalDate.of(2026, 2, 26));
        assertThat(deal.sector()).isEqualTo("Mining");
        assertThat(deal.createdDate()).isEqualTo(LocalDate.of(2025, 12, 26));
    }

    @Test
    void normalize_blankRevenueAndProbability_areNullNotZeroOrDefault() {
        // reproduces the real Deal Tracker case: "Sasuke" has blank value and blank probability
        MondayItemDto item = dealItem("Sasuke", List.of(
                col("owner_code", "OWNER_001"),
                col("client_code", "COMPANY091"),
                col("status", "Open"),
                col("probability", ""),
                col("deal_value", ""),
                col("sector", "Mining")
        ));

        NormalizedDeal deal = normalizer.normalize(item, MAPPING);

        assertThat(deal.dealValue()).isNull();
        assertThat(deal.closureProbability()).isNull();
    }

    @Test
    void normalize_blankSector_becomesUnspecified_notDropped() {
        MondayItemDto item = dealItem("Some Deal", List.of(col("sector", "")));

        NormalizedDeal deal = normalizer.normalize(item, MAPPING);

        assertThat(deal.sector()).isEqualTo("Unspecified");
    }

    @Test
    void normalize_mixedCapitalizationStatus_stillMapsCorrectly() {
        MondayItemDto item = dealItem("Deal X", List.of(col("status", "wOn")));

        assertThat(normalizer.normalize(item, MAPPING).status()).isEqualTo(DealStatus.WON);
    }

    @Test
    void normalize_unrecognizedStatusText_isUnknownNotGuessed() {
        MondayItemDto item = dealItem("Deal Y", List.of(col("status", "Deal Status")));

        assertThat(normalizer.normalize(item, MAPPING).status()).isEqualTo(DealStatus.UNKNOWN);
    }

    @Test
    void isValidRecord_rejectsStrayReEmbeddedHeaderRow() {
        // The real export contains literal rows like this where a header got re-embedded as data.
        MondayItemDto strayRow = dealItem("Deal Status", List.of());
        MondayItemDto blankNameRow = dealItem("", List.of());
        MondayItemDto realRow = dealItem("Aang", List.of());

        assertThat(normalizer.isValidRecord(strayRow)).isFalse();
        assertThat(normalizer.isValidRecord(blankNameRow)).isFalse();
        assertThat(normalizer.isValidRecord(realRow)).isTrue();
    }

    private MondayItemDto dealItem(String name, List<MondayColumnValueDto> columns) {
        return new MondayItemDto("1", name, columns);
    }

    private MondayColumnValueDto col(String id, String text) {
        return new MondayColumnValueDto(id, text, null);
    }
}
