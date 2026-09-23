package com.srm.creditengine.config;

import java.math.BigDecimal;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * JSON convention of the API contract: every {@link BigDecimal} (money and rates) is written as a
 * plain-notation string ({@code "9285.99"}, never {@code 9285.99} or {@code 9.28599E+3}) so that
 * JavaScript clients never parse it into an IEEE 754 double. Incoming decimals are accepted both as
 * strings and as JSON numbers and are read losslessly into {@link BigDecimal}.
 *
 * <p>Registered as a {@link JacksonComponent} (rather than a mapper customizer bean) so it is also
 * active in {@code @WebMvcTest} slices, which do not load {@code @Configuration} classes.
 */
@JacksonComponent
public class DecimalAsStringSerializer extends StdSerializer<BigDecimal> {

    public DecimalAsStringSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
        generator.writeString(value.toPlainString());
    }
}
