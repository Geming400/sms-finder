package fr.geming400.localisationhelper.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.IntRange;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import fr.geming400.localisationhelper.ui.activities.CustomActivity;

// TODO: Add translation stuff
public abstract class Setting<T> {
    protected final String id;
    protected String name;
    protected final T defaultValue;
    protected Category category = Category.NONE;
    protected String description = null;
    protected Set<String> requiredPermissions = new HashSet<>();

    public Setting(String id, T defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

    protected SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    abstract public T getValue(Context context);
    abstract public T getValue(Context context, T defaultVal);
    abstract public void setValue(Context context, T val);

    protected void setValueHelper(Context context, Runnable action) {
        if (hasPermissionsBeenGranted(context))
            action.run();
    }

    protected T getValueHelper(Context context, Supplier<T> action) {
        if (hasPermissionsBeenGranted(context))
            return action.get();
        else
            return defaultValue;
    }

    public String getId() {
        return id;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(@NotNull Category category) {
        this.category = Objects.requireNonNull(category);
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public String getName() {
        return name == null ? id : name;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Setting<T> addRequiredPermission(String permission) {
        if (permission == null || permission.isEmpty())
            return this;

        requiredPermissions.add(permission);
        return this;
    }

    @Unmodifiable
    public Set<String> getRequiredPermissions() {
        return Set.copyOf(requiredPermissions);
    }

    private void showPermissionsDialog(Context context, T valueToSet, Runnable callback) {
        new AlertDialog.Builder(context)
                .setTitle("Permissions required")
                .setMessage("Some permissions are required for")
                .setPositiveButton("Grant", (dialogInterface, which) -> {
                    setValue(context, valueToSet);
                    callback.run();
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> {})
                .create()
                .show();
    }

    public boolean hasPermissionsBeenGranted(Context context) {
        return requiredPermissions
                .stream()
                .allMatch(perm -> context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED);

    }

    public void askForPermissions(CustomActivity activity, T valueToSet, Runnable callback) {
        activity.requestPermissionsWithCallback(requiredPermissions.toArray(new String[0]), 0, (grantedPermissions) -> {
            if (!grantedPermissions.containsValue(false)) {
                setValue(activity, valueToSet);
                callback.run();
            }
        });
    }


    public static IntSetting ofInt(String id, int defaultValue) {
        return new IntSetting(id, defaultValue);
    }

    public static LongSetting ofLong(String id, long defaultValue) {
        return new LongSetting(id, defaultValue);
    }

    public static FloatSetting ofFloat(String id, float defaultValue) {
        return new FloatSetting(id, defaultValue);
    }

    public static BooleanSetting ofBoolean(String id, boolean defaultValue) {
        return new BooleanSetting(id, defaultValue);
    }

    public static StringSetting ofString(String id, String defaultValue) {
        return new StringSetting(id, defaultValue);
    }

    public static StringSetSetting ofStringSet(String id, Set<String> defaultValue) {
        return new StringSetSetting(id, defaultValue);
    }
    public static StringSetSetting ofStringSet(String id) {
        return new StringSetSetting(id);
    }

    public enum Category {
        NONE("None", true, 0),
        BASIC("Basic", 1),
        TEST("test", 2);

        private final String name;
        private final boolean hidden;
        @IntRange(from = 0, to = Integer.MAX_VALUE)
        private final int weight;

        Category(String name, boolean hidden, @IntRange(from = 0, to = Integer.MAX_VALUE) int weight) {
            this.name = name;
            this.hidden = hidden;
            this.weight = weight;
        }
        Category(String name, @IntRange(from = 0, to = Integer.MAX_VALUE) int weight) {
            this(name, false, weight);
        }

        public String getName() {
            return name;
        }

        public boolean isHidden() {
            return hidden;
        }

        public int getWeight() {
            return weight;
        }
    }

    public static class IntSetting extends Setting<Integer> {
        private IntSetting(String id, int defaultValue) {
            super(id, defaultValue);
        }

        @Override
        public Integer getValue(Context context) {
            return getValue(context, defaultValue);
        }

        @Override
        public Integer getValue(Context context, Integer defaultVal) {
            return getValueHelper(context, () -> getSharedPreferences(context).getInt(id, defaultVal));
        }

        @Override
        public void setValue(Context context, Integer val) {
            setValueHelper(context, () -> getSharedPreferences(context).edit()
                    .putInt(id, val)
                    .apply()
            );
        }
    }

    public static class LongSetting extends Setting<Long> {
        private LongSetting(String id, long defaultValue) {
            super(id, defaultValue);
        }

        @Override
        public Long getValue(Context context) {
            return getValue(context, defaultValue);
        }

        @Override
        public Long getValue(Context context, Long defaultVal) {
            return getValueHelper(context, () -> getSharedPreferences(context).getLong(id, defaultVal));
        }

        @Override
        public void setValue(Context context, Long val) {
            setValueHelper(context, () -> getSharedPreferences(context).edit()
                    .putLong(id, val)
                    .apply()
            );
        }
    }

    public static class FloatSetting extends Setting<Float> {
        private FloatSetting(String id, float defaultValue) {
            super(id, defaultValue);
        }

        @Override
        public Float getValue(Context context) {
            return getValue(context, defaultValue);
        }

        @Override
        public Float getValue(Context context, Float defaultVal) {
            return getValueHelper(context, () -> getSharedPreferences(context).getFloat(id, defaultVal));
        }

        @Override
        public void setValue(Context context, Float val) {
            setValueHelper(context, () -> getSharedPreferences(context).edit()
                    .putFloat(id, val)
                    .apply()
            );
        }
    }

    public static class BooleanSetting extends Setting<Boolean> {
        private BooleanSetting(String id, boolean defaultValue) {
            super(id, defaultValue);
        }

        @Override
        public Boolean getValue(Context context) {
            return getValue(context, defaultValue);
        }

        @Override
        public Boolean getValue(Context context, Boolean defaultVal) {
            return getValueHelper(context, () -> getSharedPreferences(context).getBoolean(id, defaultVal));
        }

        @Override
        public void setValue(Context context, Boolean val) {
            setValueHelper(context, () ->
                    getSharedPreferences(context).edit()
                            .putBoolean(id, val)
                            .apply()
            );
        }
    }

    public static class StringSetting extends Setting<String> {
        private StringSetting(String id, String defaultValue) {
            super(id, defaultValue);
        }

        @Override
        public String getValue(Context context) {
            return getValue(context, defaultValue);
        }

        @Override
        public String getValue(Context context, String defaultVal) {
            return getValueHelper(context, () -> getSharedPreferences(context).getString(id, defaultVal));
        }

        @Override
        public void setValue(Context context, String val) {
            setValueHelper(context, () -> getSharedPreferences(context).edit()
                    .putString(id, val)
                    .apply()
            );
        }
    }

    public static class StringSetSetting extends Setting<Set<String>> {
        private StringSetSetting(String id, Set<String> defaultValue) {
            super(id, defaultValue);
        }
        private StringSetSetting(String id) {
            this(id, Set.of());
        }

        @Override
        public Set<String> getValue(Context context) {
            return getValue(context, defaultValue);
        }

        @Override
        public Set<String> getValue(Context context, Set<String> defaultVal) {
            return getValueHelper(context, () -> getSharedPreferences(context).getStringSet(id, defaultVal));
        }

        @Override
        public void setValue(Context context, Set<String> val) {
            setValueHelper(context, () -> getSharedPreferences(context).edit()
                    .putStringSet(id, val)
                    .apply()
            );
        }
    }
}
