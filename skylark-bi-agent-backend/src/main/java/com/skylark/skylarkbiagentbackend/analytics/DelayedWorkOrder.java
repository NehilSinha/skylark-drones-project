package com.skylark.skylarkbiagentbackend.analytics;

import java.time.LocalDate;

public record DelayedWorkOrder(String dealName, String customerCode, LocalDate probableEndDate, long daysOverdue) {
}
