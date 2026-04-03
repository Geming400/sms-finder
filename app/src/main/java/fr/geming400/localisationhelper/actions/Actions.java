package fr.geming400.localisationhelper.actions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import fr.geming400.localisationhelper.Utils;

public abstract class Actions {
    private static final Map<String, Action<?>> ACTIONS = new HashMap<>();

    @SuppressLint("MissingPermission")
    public static final Action<Location> LOCATION = of("location", context -> {
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        Criteria criteria = new Criteria();
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_LOW);

        String provider = locationManager.getBestProvider(criteria, true);
        if (provider != null && Utils.hasLocationPermissions(context))
            return locationManager.getLastKnownLocation(provider);

        return null;
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

    private static <T> Action<T> of(String name, Function<Context, T> input) {
        Action<T> action = Action.of(name, input);
        ACTIONS.put(name, action);
        return action;
    }

    private Actions() {}
}
