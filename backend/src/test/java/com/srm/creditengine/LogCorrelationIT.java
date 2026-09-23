package com.srm.creditengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.support.Cnpjs;
import com.srm.creditengine.support.IntegrationTest;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Every business log line carries the trace ids and the client correlation id. */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class LogCorrelationIT {

    private static final Pattern CORRELATED_LINE =
            Pattern.compile("\\[[0-9a-f]{32},[0-9a-f]{16}] \\[cid=log-correlation-42] .*Assignor registered: id=");

    @Autowired
    private RestTestClient client;

    @Test
    void businessLogsCarryTraceAndCorrelationIdsButNoPersonalData(CapturedOutput output) {
        String cnpj = Cnpjs.random();
        client.post()
                .uri("/api/v1/assignors")
                .header("X-Correlation-Id", "log-correlation-42")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Cedente Logs Ltda\",\"document\":\"%s\"}".formatted(cnpj))
                .exchange()
                .expectStatus()
                .isCreated();

        assertThat(output.getOut()
                        .lines()
                        .anyMatch(line -> CORRELATED_LINE.matcher(line).find()))
                .as("log line with [traceId,spanId] [cid=...]")
                .isTrue();
        assertThat(output.getOut()).doesNotContain(cnpj);
    }
}
