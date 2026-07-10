package fr.geming400.localisationhelper.action;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.geming400.localisationhelper.ui.settings.Setting;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.functions.Function4;
import kotlin.jvm.functions.Function5;
import kotlin.jvm.functions.Function6;

public abstract class Action<T> extends BaseAction<CompletableFuture<@NotNull T>, T> {
    public Action(String name) {
        super(name);
    }

    public Action(String name, Setting.BooleanSetting... dependentSettings) {
        super(name, dependentSettings);
    }

    @Override
    public void sendDataSMS(@NonNull Context context, @NonNull String sender, @NonNull String privateKey) {
        CompletableFuture<String> completableFuture = this.executeAsSerialized(context);

        if (completableFuture != null)
            completableFuture.whenComplete((t, throwable) ->
                    this.smsSenderHelper(context, sender, PayloadType.DATA, t, privateKey));
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

    // Utils

    @NonNull
    protected CompletableFuture<T> futureOf(T obj) {
        return CompletableFuture.completedFuture(obj);
    }

    @NonNull
    protected CompletableFuture<T> futureOf(Supplier<T> obj) {
        return futureOf(obj.get());
    }

    protected <P> T uniTypeParsing(
            Collection<String> splittedContent,
            Function<P, T> objFactory,
            Function<String, P> parser
    ) {
        this.enforceSize(splittedContent, 1);
        return objFactory.apply(parser.apply(
                splittedContent
                        .stream()
                        .findFirst()
                        .orElseThrow()
                )
        );
    }

    protected <P> T uniTypeParsing(
            Collection<String> splittedContent,
            BiFunction<P, P, T> objFactory,
            Function<String, P> parser
    ) {
        List<P> parsedContent = this.getParsedContent(splittedContent, parser, 2);
        return objFactory.apply(
                parsedContent.get(0),
                parsedContent.get(1)
        );
    }

    protected <P> T uniTypeParsing(
            Collection<String> splittedContent,
            Function3<P, P, P, T> objFactory,
            Function<String, P> parser
    ) {
        List<P> parsedContent = this.getParsedContent(splittedContent, parser, 3);
        return objFactory.invoke(
                parsedContent.get(0),
                parsedContent.get(1),
                parsedContent.get(2)
        );
    }

    protected <P> T uniTypeParsing(
            Collection<String> splittedContent,
            Function4<P, P, P, P, T> objFactory,
            Function<String, P> parser
    ) {
        List<P> parsedContent = this.getParsedContent(splittedContent, parser, 4);
        return objFactory.invoke(
                parsedContent.get(0),
                parsedContent.get(1),
                parsedContent.get(2),
                parsedContent.get(3)
        );
    }

    protected <P> T uniTypeParsing(
            Collection<String> splittedContent,
            Function5<P, P, P, P, P, T> objFactory,
            Function<String, P> parser
    ) {
        List<P> parsedContent = this.getParsedContent(splittedContent, parser, 5);
        return objFactory.invoke(
                parsedContent.get(0),
                parsedContent.get(1),
                parsedContent.get(2),
                parsedContent.get(3),
                parsedContent.get(4)
        );
    }

    protected <P> T uniTypeParsing(
            Collection<String> splittedContent,
            Function6<P, P, P, P, P, P, T> objFactory,
            Function<String, P> parser
    ) {
        List<P> parsedContent = this.getParsedContent(splittedContent, parser, 6);
        return objFactory.invoke(
                parsedContent.get(0),
                parsedContent.get(1),
                parsedContent.get(2),
                parsedContent.get(3),
                parsedContent.get(4),
                parsedContent.get(5)
        );
    }

    private void enforceSize(Collection<String> splittedContent, int size) {
        if (splittedContent.size() != size)
            throw new RuntimeException("Collection " + splittedContent + " is expected to be of size " + size + " but is actually of size " + splittedContent.size());
    }

    private <P> List<P> getParsedContent(Collection<String> splittedContent, Function<String, P> parser, int exceptedSize) {
        this.enforceSize(splittedContent, exceptedSize);

        List<P> parsedContent = new ArrayList<>();
        for (String str : splittedContent)
            parsedContent.add(parser.apply(str));

        return parsedContent;
    }
}
