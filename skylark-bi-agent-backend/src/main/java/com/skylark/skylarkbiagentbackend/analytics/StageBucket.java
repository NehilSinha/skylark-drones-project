package com.skylark.skylarkbiagentbackend.analytics;

import java.math.BigDecimal;

public record StageBucket(String stage, long count, BigDecimal value) {
}
