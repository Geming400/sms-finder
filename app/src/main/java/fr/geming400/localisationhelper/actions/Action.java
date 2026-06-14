package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import fr.geming400.localisationhelper.utils.Utils;

public abstract class Action<T> extends BaseAction<CompletableFuture<T>, T> {
    public Action(String name) {
        super(name);
    }

    @Override
    public void sendSMS(@NonNull Context context, @NonNull String sender) {
        CompletableFuture<String> completableFuture = this.executeAsSerialized(context);

        if (completableFuture != null)
            completableFuture.whenComplete((t, throwable) ->
                Utils.sendSMS(context, sender, this.getName() + ":" + t));
    }

    @Nullable
    @Override
    public abstract CompletableFuture<T> execute(Context context);

    @Nullable
    public CompletableFuture<String> executeAsSerialized(Context context) {
        CompletableFuture<T> completableFuture = this.execute(context);
        if (completableFuture == null)
            return null;

        return this.serializeFuture(completableFuture, this::serializeResult);
    }

    @NonNull
    public abstract String serializeResult(T obj);

    protected <C> CompletableFuture<String> serializeFuture(CompletableFuture<C> completableFuture, Function<C, String> serializer) {
        CompletableFuture<String> stringCompletableFuture = new CompletableFuture<>();
        completableFuture.whenComplete((t, throwable) -> {
            if (throwable == null) {
                stringCompletableFuture.complete(serializer.apply(t));
            } else {
                String exceptionMsg = String.format("Tried to serialize future %s with Action#serializeFuture but failed", completableFuture);
                throw new RuntimeException(exceptionMsg, throwable);
            }
        });

        return stringCompletableFuture;
    }
}
