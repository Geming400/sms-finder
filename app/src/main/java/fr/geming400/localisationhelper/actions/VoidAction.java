package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.function.Consumer;

public abstract class VoidAction extends BaseAction<Void> {
    public VoidAction(String name) {
        super(name);
    }

    @Nullable
    @Override
    public Void execute(Context context) {
        executeVoid(context);
        return null;
    }

    public abstract void executeVoid(Context context);

    public static VoidAction of(String name, Consumer<Context> input) {
        return new VoidAction(name) {
            @Override
            public void executeVoid(Context context) {
                input.accept(context);
            }
        };
    }
}
