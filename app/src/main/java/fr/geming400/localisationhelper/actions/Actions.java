package fr.geming400.localisationhelper.actions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.Nullable;
import androidx.core.location.LocationRequestCompat;

import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import fr.geming400.localisationhelper.Utils;

public abstract class Actions {
    private static final Map<String, BaseAction<?>> ACTIONS = new HashMap<>();

    @SuppressLint("MissingPermission")
    public static final Action<Location> LOCATION = of("location", context -> {
        LocationManager locationManager = context.getSystemService(LocationManager.class);

        boolean hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        CompletableFuture<Location> completableFuture = new CompletableFuture<>();

        if (hasGps && Utils.hasLocationPermissions(context)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LocationRequestCompat.PASSIVE_INTERVAL,
                    Integer.MAX_VALUE,
                    completableFuture::complete
            );

            return completableFuture;
        }

        return null;
    });

    public static final VoidAction PING = ofVoid("ping");

    @Nullable
    public static <T> BaseAction<T> getByName(String name) {
        BaseAction<?> action = ACTIONS.get(name);
        try {
            // noinspection unchecked<
            return (BaseAction<T>) action;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static BaseAction<?> getByNameTypeless(String name) {
        return ACTIONS.get(name);
    }


    @Unmodifiable
    public static Map<String, BaseAction<?>> getAllActions() {
        return Map.copyOf(ACTIONS);
    }

    private static <T> Action<T> of(String name, Function<Context, CompletableFuture<T>> input) {
        Action<T> action = Action.of(name, input);
        ACTIONS.put(name, action);
        return action;
    }
    private static VoidAction ofVoid(String name, Consumer<Context> input) {
        VoidAction action = VoidAction.of(name, input);
        ACTIONS.put(name, action);
        return action;
    }
    private static VoidAction ofVoid(String name) {
        return ofVoid(name, context -> {});
    }

    private Actions() {}
}
