package com.skylark.skylarkbiagentbackend.client.monday;

import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.exception.MondayRateLimitException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MondayRateLimiterTest {

    @Test
    void acquire_succeedsImmediatelyWhenTokensAreAvailable() {
        MondayRateLimiter limiter = new MondayRateLimiter(propertiesWithPermitsPerMinute(60));

        assertThatCode(limiter::acquire).doesNotThrowAnyException();
    }

    @Test
    void acquire_failsFastWhenBudgetIsExhaustedAndWaitWouldBeLong() {
        // 1 permit/minute: the first call consumes it, the second would need a ~60s
        // wait, which is well past the client's short-wait tolerance — it should
        // fail fast rather than block the calling thread for a minute.
        MondayRateLimiter limiter = new MondayRateLimiter(propertiesWithPermitsPerMinute(1));

        limiter.acquire();

        assertThatThrownBy(limiter::acquire).isInstanceOf(MondayRateLimitException.class);
    }

    private MondayProperties propertiesWithPermitsPerMinute(int permitsPerMinute) {
        return new MondayProperties(
                new MondayProperties.Api("http://localhost", "token", 1000, 1000),
                new MondayProperties.Boards("1", "2"),
                new MondayProperties.Pagination(100, 10),
                new MondayProperties.RateLimit(permitsPerMinute),
                new MondayProperties.Retry(3, 10, 2.0)
        );
    }
}
