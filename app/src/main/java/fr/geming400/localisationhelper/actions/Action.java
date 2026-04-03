package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
    public abstract T execute(Context context);

    public static <T> Action<T> of(String name, Function<Context, T> input) {
        return new Action<>(name) {
            @Nullable
            @Override
            public T execute(Context context) {
                return input.apply(context);
            }
        };
    }
}
