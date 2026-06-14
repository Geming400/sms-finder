package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

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

    @Override
    public void sendSMS(@NonNull Context context, @NonNull String sender) {
        this.sendSmsHelper(context, sender, null);
    }

    @NonNull
    @Override
    public final Boolean parse(String rawContent) throws MalformedRawActionException {
        return rawContent.equals(this.getName());
    }

    public static VoidAction of(String name, Consumer<Context> input) {
        return new VoidAction(name) {
            @Override
            public void executeVoid(Context context) {
                input.accept(context);
            }
        };
    }
}
