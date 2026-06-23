package fr.geming400.localisationhelper.ui.settings;

import android.Manifest;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.geming400.localisationhelper.R;

public final class Settings {
    public static final Map<String, Setting<?>> SETTINGS = new HashMap<>();

    public static final Setting.BooleanSetting LOCATION = register(
            Setting.ofBoolean("location", false),
            Setting.Category.ACTIONS,
            R.string.setting_geolocation_name,
            R.string.setting_geolocation_description,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    );

    public static final Setting.BooleanSetting BATTERY = register(
            Setting.ofBoolean("battery", false),
            Setting.Category.ACTIONS,
            R.string.setting_battery_name,
            R.string.setting_battery_description
    );

    private static <T, S extends Setting<T>> S register(S setting, Setting.Category category) {
        return register(setting, category, null, null);
    }

    private static <T, S extends Setting<T>> S register(
            S setting,
            Setting.Category category,
            @Nullable @StringRes Integer name,
            @Nullable @StringRes Integer description,
            String... requiredPermissions
    ) {
        setting.setCategory(category);
        setting.setName(name);
        setting.setDescription(description);

        Arrays.asList(requiredPermissions)
                .forEach(setting::addRequiredPermission);

        SETTINGS.put(setting.getId(), setting);
        return setting;
    }

    @Unmodifiable
    public static Map<Setting.Category, List<Setting<?>>> getSettingsByCategory() {
        Map<Setting.Category, List<Setting<?>>> res = new LinkedHashMap<>(SETTINGS.size());
        Collection<Setting<?>> settingsSortedByCategoryWeight = SETTINGS.values()
                .stream()
                .sorted(Comparator.comparingInt(setting -> setting.category.getOrder()))
                .collect(Collectors.toList());

        for (Setting<?> setting : settingsSortedByCategoryWeight) {
            List<Setting<?>> settings = res.getOrDefault(setting.getCategory(), new ArrayList<>());

            //noinspection DataFlowIssue
            settings.add(setting);
            res.put(
                    setting.getCategory(),
                    settings
            );
        }

        return res;
    }

    @Unmodifiable
    public static Collection<Setting<?>> getSettings() {
        return List.copyOf(SETTINGS.values());
    }
    
    private Settings() {}
}
