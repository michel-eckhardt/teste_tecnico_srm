package com.srm.creditengine.web.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strong ETags derived from the optimistic locking version: {@code ETag: "3"}. {@code If-Match}
 * accepts the quoted form (and, leniently, a weak {@code W/"3"} or a bare {@code 3}); the wildcard
 * {@code *} is refused because commands must name the exact version they were based on.
 */
public final class EntityTags {

    private static final Pattern VERSION_TAG = Pattern.compile("^(?:W/)?\"?(\\d{1,18})\"?$");

    private EntityTags() {}

    public static String of(long version) {
        return "\"" + version + "\"";
    }

    /**
     * @throws InvalidHeaderException when the header does not carry a single version
     */
    public static long parseIfMatch(String ifMatch) {
        Matcher matcher = VERSION_TAG.matcher(ifMatch.strip());
        if (!matcher.matches()) {
            throw new InvalidHeaderException(
                    "If-Match", "deve conter a versão atual do recurso, por exemplo \"0\" (valor do ETag)");
        }
        return Long.parseLong(matcher.group(1));
    }
}
