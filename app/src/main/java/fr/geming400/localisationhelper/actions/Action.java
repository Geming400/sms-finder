package fr.geming400.localisationhelper.actions;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Action<T> {
    private final String name;

    public Action(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Gets the name of this with the password hashed using {@code SHA-256}
     * @param password the password
     * @return the action's name in the format {@code "hashed pass/nam"}
     * @implNote if the {@code SHA-256} algorithm doesn't exist, only the {@linkplain #getName() name} will be returned
     */
    public String getName(String password) {
        try {
            return this.digestPasswordAsSha256(password) + "/" + this.getName();
        } catch (NoSuchAlgorithmException e) {
            return this.getName();
        }
    }

    protected String digestPasswordAsSha256(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Arrays.toString(md.digest(password.getBytes(StandardCharsets.US_ASCII)));
    }

    @Nullable
    public abstract CompletableFuture<T> execute(Context context);

    public abstract JsonElement asJson(Context context, T val);
    public CompletableFuture<JsonElement> asJson(Context context) {
        CompletableFuture<JsonElement> completableFuture = new CompletableFuture<>();
        CompletableFuture<T> execResult = execute(context);

        if (execResult == null) {
            completableFuture.complete(new JsonObject());
        } else {
            execResult.whenComplete((val, exception) ->
                    completableFuture.complete(asJson(context, val)));
        }

        return completableFuture;
    }

    public String jsonToBase64(Context context) {
        return Base64.encodeToString(asJson(context).toString().getBytes(), Base64.URL_SAFE);
    }

    public static <T> Action<T> of(String name, Function<Context, CompletableFuture<T>> input, BiFunction<@NotNull Context, @NotNull T, JsonElement> jsonConverter) {
        return new Action<>(name) {
            @Nullable
            @Override
            public CompletableFuture<T> execute(Context context) {
                return input.apply(context);
            }

            @Override
            public JsonElement asJson(Context context, T val) {
                return jsonConverter.apply(context, val);
            }
        };
    }
}
