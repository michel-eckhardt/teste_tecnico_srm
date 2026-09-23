package com.srm.creditengine.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void reusesAWellFormedIdAndExposesItInTheMdcDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "req-42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInMdc = new AtomicReference<>();

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seenInMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            }
        });

        assertThat(seenInMdc).hasValue("req-42");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("req-42");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesAnIdWhenAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).hasSize(36);
    }

    @Test
    void replacesUnsafeIdsToPreventLogInjection() {
        assertThat(CorrelationIdFilter.resolve("abc\r\nFAKE LOG LINE")).hasSize(36);
        assertThat(CorrelationIdFilter.resolve("x".repeat(65))).hasSize(36);
    }
}
