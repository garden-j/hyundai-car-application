package com.example.application.core.pleos;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

/**
 * Pleos SDK는 Kotlin 람다((T) -> Unit)를 콜백으로 받는다.
 * Java에서 매번 {@code return Unit.INSTANCE;}를 쓰지 않도록 변환 도우미를 둔다.
 */
public final class Kt {
    private Kt() {
    }

    public static Function0<Unit> f0(Runnable r) {
        return () -> {
            r.run();
            return Unit.INSTANCE;
        };
    }

    public static <T> Function1<T, Unit> f1(Consumer<T> c) {
        return t -> {
            c.accept(t);
            return Unit.INSTANCE;
        };
    }

    public static <A, B> Function2<A, B, Unit> f2(BiConsumer<A, B> c) {
        return (a, b) -> {
            c.accept(a, b);
            return Unit.INSTANCE;
        };
    }
}
