package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class Action<T> extends BaseAction<CompletableFuture<T>, T> {
    public Action(String name) {
        super(name);
    }

    @Override
    public void sendDataSMS(@NonNull Context context, @NonNull String sender) {
        CompletableFuture<String> completableFuture = this.executeAsSerialized(context);

        if (completableFuture != null)
            completableFuture.whenComplete((t, throwable) ->
                    this.smsSenderHelper(context, sender, PayloadType.INSTRUCTION, t));
    }

    @Nullable
    public CompletableFuture<String> executeAsSerialized(Context context) {
        CompletableFuture<T> completableFuture = this.execute(context);
        if (completableFuture == null)
            return null;

        return this.serializeFuture(completableFuture, this::serializeResult);
    }

    @NonNull
    public abstract String serializeResult(@NonNull T obj);

    /**
     * Parses the raw content of the action to {@link T}.
     * @param rawContent the raw content of the action
     * @return the parsed content
     * @throws MalformedRawActionException if there was an error while parsing the raw content
     */
    @NonNull
    public abstract T parse(@NonNull String rawContent) throws MalformedRawActionException;

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
