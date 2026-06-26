package fr.geming400.localisationhelper.action;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public interface HasPayloadType {
    /**
     * Gets the {@link PayloadType PayloadType} linked to this object
     * @return the {@link PayloadType PayloadType} linked to this object
     */
    @Contract(pure = true)
    @NonNull
    PayloadType getPayloadType();
}
