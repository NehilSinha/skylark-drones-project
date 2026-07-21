package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.agent.router.DateExpressionResolver;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResolveDateExpressionToolTest {

    private final Clock clock = Clock.fixed(
            LocalDate.of(2026, 7, 21).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private final ToolInvocationTracker tracker = new ToolInvocationTracker();
    private final ResolveDateExpressionTool tool =
            new ResolveDateExpressionTool(new DateExpressionResolver(clock), tracker);

    @Test
    void resolveDateExpression_delegatesAndRecordsInvocation() {
        DateRange range = tool.resolveDateExpression("this quarter");

        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(tracker.drain()).containsExactly("ResolveDateExpressionTool");
    }

    @Test
    void resolveDateExpression_unrecognizedPhrase_propagatesValidationException() {
        assertThatThrownBy(() -> tool.resolveDateExpression("next fiscal decade"))
                .isInstanceOf(ValidationException.class);
    }
}
