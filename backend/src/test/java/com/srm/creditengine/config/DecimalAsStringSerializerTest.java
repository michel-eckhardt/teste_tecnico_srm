package com.srm.creditengine.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

class DecimalAsStringSerializerTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new SimpleModule().addSerializer(new DecimalAsStringSerializer()))
            .build();

    record Amount(BigDecimal value, int count) {}

    @Test
    void writesDecimalsAsPlainStringsAndKeepsOtherNumbersNumeric() {
        String json = mapper.writeValueAsString(new Amount(new BigDecimal("9285.99"), 2));

        assertThat(json).isEqualTo("{\"value\":\"9285.99\",\"count\":2}");
    }

    @Test
    void neverUsesScientificNotation() {
        String json = mapper.writeValueAsString(Map.of("value", new BigDecimal("1E+3")));

        assertThat(json).isEqualTo("{\"value\":\"1000\"}");
    }

    @Test
    void keepsTheScaleOfTheValue() {
        String json = mapper.writeValueAsString(Map.of("rate", new BigDecimal("0.01500000")));

        assertThat(json).isEqualTo("{\"rate\":\"0.01500000\"}");
    }

    @Test
    void readsDecimalsFromStringsAndNumbersWithoutPrecisionLoss() {
        Amount fromString = mapper.readValue("{\"value\":\"0.10000000000000000001\",\"count\":1}", Amount.class);
        Amount fromNumber = mapper.readValue("{\"value\":0.10000000000000000001,\"count\":1}", Amount.class);

        assertThat(fromString.value()).isEqualTo(new BigDecimal("0.10000000000000000001"));
        assertThat(fromNumber.value()).isEqualTo(new BigDecimal("0.10000000000000000001"));
    }
}
