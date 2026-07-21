package com.skylark.skylarkbiagentbackend.normalizer;

import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayColumnValueDto;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.config.WorkOrderColumnMapping;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkOrderNormalizerTest {

    private static final WorkOrderColumnMapping MAPPING = new WorkOrderColumnMapping(
            "customer_code", "owner_code", "sector", "execution_status", "probable_end_date",
            "data_delivery_date", "billing_status", "invoice_status", "amount_to_be_billed",
            "collected_amount", "amount_receivable", "quantity_as_per_po", "quantity_billed");

    private final WorkOrderNormalizer normalizer = new WorkOrderNormalizer();

    @Test
    void normalize_fullyPopulatedRow() {
        MondayItemDto item = woItem("Scooby-Doo", List.of(
                col("customer_code", "WOCOMPANY_002"),
                col("owner_code", "OWNER_003"),
                col("sector", "Mining"),
                col("execution_status", "Completed"),
                col("billing_status", "Not Billable"),
                col("amount_to_be_billed", "264398.08")
        ));

        NormalizedWorkOrder wo = normalizer.normalize(item, MAPPING);

        assertThat(wo.dealName()).isEqualTo("Scooby-Doo");
        assertThat(wo.customerCode()).isEqualTo("WOCOMPANY_002");
        assertThat(wo.ownerCode()).isEqualTo("OWNER_003");
        assertThat(wo.sector()).isEqualTo("Mining");
        assertThat(wo.executionStatus()).isEqualTo("Completed");
        assertThat(wo.billingStatus()).isEqualTo("Not Billable");
        assertThat(wo.amountToBeBilledExclGst()).isEqualTo(new BigDecimal("264398.08"));
    }

    @Test
    void normalize_billingStatusCasingCollision_collapsesToCanonicalValue() {
        MondayItemDto billed = woItem("A", List.of(col("billing_status", "Billed")));
        MondayItemDto bIlled = woItem("B", List.of(col("billing_status", "BIlled")));

        assertThat(normalizer.normalize(billed, MAPPING).billingStatus()).isEqualTo("Billed");
        assertThat(normalizer.normalize(bIlled, MAPPING).billingStatus()).isEqualTo("Billed");
    }

    @Test
    void normalize_blankStatus_becomesNotSet_notNull() {
        MondayItemDto item = woItem("A", List.of(col("billing_status", "")));

        assertThat(normalizer.normalize(item, MAPPING).billingStatus()).isEqualTo("Not Set");
    }

    @Test
    void normalize_blankSector_becomesUnspecified() {
        MondayItemDto item = woItem("A", List.of(col("sector", "")));

        assertThat(normalizer.normalize(item, MAPPING).sector()).isEqualTo("Unspecified");
    }

    @Test
    void normalize_quantityWithUnit_extractsNumericPortion() {
        // reproduces the real "5360 HA" value from the Work Order Tracker export
        MondayItemDto item = woItem("A", List.of(col("quantity_as_per_po", "5360 HA")));

        assertThat(normalizer.normalize(item, MAPPING).quantityAsPerPo()).isEqualByComparingTo("5360");
    }

    @Test
    void isValidRecord_rejectsStrayHeaderRowAndBlankName() {
        assertThat(normalizer.isValidRecord(woItem("Execution Status", List.of()))).isFalse();
        assertThat(normalizer.isValidRecord(woItem("", List.of()))).isFalse();
        assertThat(normalizer.isValidRecord(woItem("Scooby-Doo", List.of()))).isTrue();
    }

    private MondayItemDto woItem(String name, List<MondayColumnValueDto> columns) {
        return new MondayItemDto("1", name, columns);
    }

    private MondayColumnValueDto col(String id, String text) {
        return new MondayColumnValueDto(id, text, null);
    }
}
