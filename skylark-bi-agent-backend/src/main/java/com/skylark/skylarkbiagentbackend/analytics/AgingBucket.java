package com.skylark.skylarkbiagentbackend.analytics;

import java.math.BigDecimal;

public record AgingBucket(String label, long count, BigDecimal value) {
}
