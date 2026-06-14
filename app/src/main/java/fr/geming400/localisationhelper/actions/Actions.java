package fr.geming400.localisationhelper.actions;

import android.content.Context;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Actions {
    private static final Map<String, BaseAction<?, ?>> ACTIONS = new HashMap<>();

    public static final LocationGetterAction LOCATION = of("location", LocationGetterAction::new);

    public static final VoidAction PING = ofVoid("ping");

    @Nullable
    public static <T, P> BaseAction<T, P> getByName(String name) {
        try {
            // noinspection unchecked
            return (BaseAction<T, P>) ACTIONS.get(name);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static BaseAction<?, ?> getByNameTypeless(String name) {
        return ACTIONS.get(name);
    }


    @Unmodifiable
    public static Map<String, BaseAction<?, ?>> getAllActions() {
        return Map.copyOf(ACTIONS);
    }

    private static <T, A extends Action<T>> A of(String name, Function<String, A> actionFactory) {
        A action = actionFactory.apply(name);
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
