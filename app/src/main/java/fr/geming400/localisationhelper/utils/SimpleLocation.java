package fr.geming400.localisationhelper.utils;

import android.location.Location;

import androidx.annotation.FloatRange;

import java.util.Objects;

public final class SimpleLocation {
    @FloatRange(from = -90, to = 90)
    private final double latitude;
    @FloatRange(from = -180, to = 180)
    private final double longitude;

    public SimpleLocation(@FloatRange(from = -90, to = 90) double latitude, @FloatRange(from = -180, to = 180) double longitude) {
        this.latitude = Math.clamp(latitude, -90, 90);
        this.longitude = Math.clamp(longitude, -180, 180);
    }

    @FloatRange(from = -90, to = 90)
    public double getLatitude() {
        return this.latitude;
    }

    @FloatRange(from = -180, to = 180)
    public double getLongitude() {
        return this.longitude;
    }

    @Override
    public String toString() {
        return "SimpleLocation{" +
                "latitude=" + this.latitude +
                ", longitude=" + this.longitude +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SimpleLocation that = (SimpleLocation) o;
        return Double.compare(this.latitude, that.latitude) == 0 && Double.compare(this.longitude, that.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.latitude, this.longitude);
    }

    public static SimpleLocation ofLocation(Location location) {
        return new SimpleLocation(location.getLatitude(), location.getLongitude());
    }
}
