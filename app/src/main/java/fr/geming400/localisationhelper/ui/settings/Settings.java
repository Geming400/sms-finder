package fr.geming400.localisationhelper.ui.settings;

import android.Manifest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Settings {
    public static final Map<String, Setting<?>> SETTINGS = new HashMap<>();
    
    public static final Setting.BooleanSetting LOCATION = register(Setting.ofBoolean("location", false), Setting.Category.BASIC, "If geolocation should be enabled", Manifest.permission.READ_CONTACTS);
    public static final Setting.StringSetting STR = register(Setting.ofString("str", "Hii :3"), Setting.Category.BASIC);
    public static final Setting.FloatSetting FLOAT = register(Setting.ofFloat("FLOAT", .5f), Setting.Category.BASIC);

    public static final Setting.IntSetting INT2 = register(Setting.ofInt("int2", 5), Setting.Category.TEST);

    private static <T, S extends Setting<T>> S register(S setting, Setting.Category category) {
        return register(setting, category, null);
    }

    private static <T, S extends Setting<T>> S register(S setting, Setting.Category category, String description, String... requiredPermissions) {
        setting.setCategory(category);
        setting.setDescription(description);
        Arrays.asList(requiredPermissions).forEach(setting::addRequiredPermission);

        SETTINGS.put(setting.getId(), setting);
        return setting;
    }

    public static Map<Setting.Category, List<Setting<?>>> getSettingsByCategory() {
        Map<Setting.Category, List<Setting<?>>> res = new LinkedHashMap<>(SETTINGS.size());
        Collection<Setting<?>> settingsSortedByCategoryWeight = SETTINGS.values()
                .stream()
                .sorted(Comparator.comparingInt(setting -> setting.category.getWeight()))
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
    
    private Settings() {}
}
