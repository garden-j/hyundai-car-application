package com.example.application.data.direct.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** API 28에서 쓸 수 없는 List.of / Set.of 대체(불변 컬렉션). */
public final class Lists {
    private Lists() {
    }

    @SafeVarargs
    public static <T> List<T> of(T... items) {
        return items.length == 0 ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(items));
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... items) {
        return items.length == 0 ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(items)));
    }

    public static <E extends Enum<E>> Set<E> emptyEnumSet(Class<E> type) {
        return Collections.unmodifiableSet(EnumSet.noneOf(type));
    }
}
