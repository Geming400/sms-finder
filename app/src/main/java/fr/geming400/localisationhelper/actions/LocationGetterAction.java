package fr.geming400.localisationhelper.actions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.location.LocationRequestCompat;

import java.util.concurrent.CompletableFuture;

import fr.geming400.localisationhelper.utils.SimpleLocation;
import fr.geming400.localisationhelper.utils.Utils;

/**
 * Serialization format: {@code "latitude;altitude"}
 */
public final class LocationGetterAction extends Action<SimpleLocation> {
    public LocationGetterAction(String name) {
        super(name);
    }

    @Override
    @NonNull
    public SimpleLocation parse(String rawContent) throws MalformedRawActionException {
        String[] geolocation = rawContent.split(";");
        if (geolocation.length == 2) {
            try {
                double latitude = Math.clamp(Double.parseDouble(geolocation[0]), -90, 90);
                double longitude = Math.clamp(Double.parseDouble(geolocation[0]), -180, 180);

                return new SimpleLocation(latitude, longitude);
            } catch (NumberFormatException e) {
                throw new MalformedRawActionException("Couldn't parse latitude/longitude doubles", e, this, rawContent);
            }
        } else {
            throw new MalformedRawActionException("Geolocation data doesn't contain 2 entries", this, rawContent);
        }
    }

    @NonNull
    @Override
    public String serializeResult(SimpleLocation location) {
        return String.format("%s;%s", location.getLatitude(), location.getLongitude());
    }

    @Nullable
    @Override
    @SuppressLint("MissingPermission")
    public CompletableFuture<SimpleLocation> execute(Context context) {
        LocationManager locationManager = context.getSystemService(LocationManager.class);

        boolean hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        CompletableFuture<SimpleLocation> completableFuture = new CompletableFuture<>();

        if (hasGps && Utils.hasLocationPermissions(context)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LocationRequestCompat.PASSIVE_INTERVAL,
                    Integer.MAX_VALUE,
                    location -> completableFuture.complete(SimpleLocation.ofLocation(location))
            );

            return completableFuture;
        }

        return null;
    }
}
