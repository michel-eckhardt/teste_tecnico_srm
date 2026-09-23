package com.srm.creditengine.web.support;

/** Strong ETags derived from the optimistic locking version: {@code ETag: "3"}. */
public final class EntityTags {

    private EntityTags() {}

    public static String of(long version) {
        return "\"" + version + "\"";
    }
}
