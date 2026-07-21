package com.skylark.skylarkbiagentbackend.agent.tool;

import com.skylark.skylarkbiagentbackend.agent.router.DateExpressionResolver;
import com.skylark.skylarkbiagentbackend.analytics.ForecastResponse;
import com.skylark.skylarkbiagentbackend.analytics.ForecastService;
import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForecastToolTest {

    private final Clock clock = Clock.fixed(
            LocalDate.of(2026, 7, 21).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Test
    void getForecast_resolvesPhraseInternally_delegatesToServiceAndRecordsInvocation() {
        ForecastService service = Mockito.mock(ForecastService.class);
        ToolInvocationTracker tracker = new ToolInvocationTracker();
        ForecastTool tool = new ForecastTool(service, new DateExpressionResolver(clock), tracker);

        DateRange expectedHorizon = new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30), "this quarter");
        ForecastResponse expected = new ForecastResponse(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, 0, List.of());
        when(service.forecast(expectedHorizon, null)).thenReturn(expected);

        ForecastResponse actual = tool.getForecast("this quarter", null);

        assertThat(actual).isSameAs(expected);
        verify(service).forecast(expectedHorizon, null);
        assertThat(tracker.drain()).containsExactly("ForecastTool");
    }

    @Test
    void getForecast_unrecognizedPhrase_propagatesValidationExceptionWithoutCallingService() {
        ForecastService service = Mockito.mock(ForecastService.class);
        ForecastTool tool = new ForecastTool(service, new DateExpressionResolver(clock), new ToolInvocationTracker());

        assertThatThrownBy(() -> tool.getForecast("next fiscal decade", null))
                .isInstanceOf(ValidationException.class);
    }
}
