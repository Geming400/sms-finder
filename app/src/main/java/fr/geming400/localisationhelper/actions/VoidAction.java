package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class VoidAction extends BaseAction<Void, Boolean> {
    public VoidAction(String name) {
        super(name);
    }

    @Nullable
    @Override
    public final Void execute(Context context) {
        this.executeVoid(context);
        return null;
    }

    public abstract void executeVoid(Context context);

    /**
     * Checks if the {@code rawContent} of the action is valid
     * @param rawContent the raw content of the action
     * @return if the {@code rawContent} is valid
     * @throws MalformedRawActionException if there was an error while parsing/checking the raw content
     */
    @NonNull
    public abstract Boolean checkValidity(@NonNull String rawContent) throws MalformedRawActionException;
}
