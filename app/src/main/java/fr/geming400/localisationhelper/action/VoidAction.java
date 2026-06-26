package fr.geming400.localisationhelper.action;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.geming400.localisationhelper.ui.settings.Setting;

public abstract class VoidAction extends BaseAction<Void, Boolean> {
    public VoidAction(String name) {
        super(name);
    }

    public VoidAction(String name, Setting.BooleanSetting... dependentSettings) {
        super(name, dependentSettings);
    }

    @Nullable
    @Override
    public final Void execute(@NonNull Context context) {
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
