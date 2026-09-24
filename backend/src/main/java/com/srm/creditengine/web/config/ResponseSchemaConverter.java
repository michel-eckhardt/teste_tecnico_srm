package com.srm.creditengine.web.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Makes the response models of the OpenAPI document state what the API guarantees: every field of
 * a response record is always present ({@code required}, absent values are serialized as an
 * explicit {@code null}) and only the components annotated with JSpecify {@link Nullable} may be
 * {@code null} (OpenAPI 3.1 {@code oneOf} with {@code type: "null"}). Without it every response
 * field is optional, so generated clients would have to null-check everything.
 *
 * <p>Response models are the records of the {@code web} package whose name does not end with
 * {@code Request}, {@code Query} or {@code Params}; request models keep the requirements derived from
 * their Bean Validation constraints.
 */
@Component
class ResponseSchemaConverter implements ModelConverter {

    private static final String WEB_PACKAGE = "com.srm.creditengine.web";
    private static final List<String> REQUEST_SUFFIXES = List.of("Request", "Query", "Params");
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    @Override
    public @Nullable Schema<?> resolve(
            AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }
        Schema<?> schema = chain.next().resolve(type, context, chain);
        Class<?> rawClass = Json.mapper().constructType(type.getType()).getRawClass();
        if (schema != null && isResponseRecord(rawClass)) {
            Schema<?> model = definedModel(schema, context);
            if (model != null && model.getProperties() != null) {
                applyContract(rawClass, model);
            }
        }
        return schema;
    }

    private static boolean isResponseRecord(Class<?> type) {
        return type.isRecord()
                && type.getPackageName().startsWith(WEB_PACKAGE)
                && REQUEST_SUFFIXES.stream().noneMatch(type.getSimpleName()::endsWith);
    }

    /** The resolver returns either the model itself or a reference to the model it registered. */
    private static @Nullable Schema<?> definedModel(Schema<?> schema, ModelConverterContext context) {
        String ref = schema.get$ref();
        if (ref == null) {
            return schema;
        }
        return context.getDefinedModels().get(ref.substring(ref.lastIndexOf('/') + 1));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyContract(Class<?> record, Schema model) {
        Map<String, Schema> properties = model.getProperties();
        List<String> required = new ArrayList<>();
        for (RecordComponent component : record.getRecordComponents()) {
            Schema property = properties.get(component.getName());
            if (property == null) {
                continue;
            }
            // absent values are serialized as an explicit null, so nullable fields are required too
            required.add(component.getName());
            if (component.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                properties.put(component.getName(), nullable(property));
            }
        }
        model.setRequired(required);
    }

    /**
     * Wraps instead of mutating: property schemas may be shared instances (e.g. the decimal schema
     * that replaces {@code BigDecimal}), so adding {@code "null"} to one would leak into others.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema nullable(Schema property) {
        String ref = property.get$ref();
        Schema value =
                ref == null ? property : new JsonSchema().$ref(ref.startsWith("#") ? ref : SCHEMA_REF_PREFIX + ref);
        return new JsonSchema()
                .oneOf(List.of(value, new JsonSchema().types(Set.of("null"))))
                .description(property.getDescription());
    }
}
