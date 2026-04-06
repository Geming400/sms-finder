package fr.geming400.localisationhelper.actions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.Nullable;
import androidx.core.location.LocationRequestCompat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import fr.geming400.localisationhelper.Utils;

public abstract class Actions {
    private static final Map<String, Action<?>> ACTIONS = new HashMap<>();

    @SuppressLint("MissingPermission")
    public static final Action<Location> LOCATION = of("location",
            context -> {
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
            }, (context, location) -> {
                JsonObject object = new JsonObject();
                object.addProperty("altitude", location.getAltitude());
                object.addProperty("longitude", location.getLongitude());

                return object;
            });

    @Nullable
    public static <T> Action<T> getByName(String name) {
        Action<?> action = ACTIONS.get(name);
        try {
            // noinspection unchecked
            return (Action<T>) action;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static Action<?> getByNameTypeless(String name) {
        return ACTIONS.get(name);
    }


    @Unmodifiable
    public static Map<String, Action<?>> getAllActions() {
        return Map.copyOf(ACTIONS);
    }

    private static <T> Action<T> of(String name, Function<Context, CompletableFuture<T>> input, BiFunction<@NotNull Context, T, JsonElement> jsonConverter) {
        Action<T> action = Action.of(name, input, jsonConverter);
        ACTIONS.put(name, action);
        return action;
    }

    private Actions() {}
}
