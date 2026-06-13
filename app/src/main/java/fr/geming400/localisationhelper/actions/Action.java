package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class Action<T> extends BaseAction<CompletableFuture<T>> {
    public Action(String name) {
        super(name);
    }

    public static <T> Action<T> of(String name, Function<Context, CompletableFuture<T>> input) {
        return new Action<>(name) {
            @Nullable
            @Override
            public CompletableFuture<T> execute(Context context) {
                return input.apply(context);
            }
        };
    }
}
