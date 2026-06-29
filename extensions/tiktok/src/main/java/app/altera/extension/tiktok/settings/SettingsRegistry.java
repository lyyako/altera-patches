package app.altera.extension.tiktok.settings;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
public final class SettingsRegistry {
    private static final List<SettingsEntry> SETTINGS = new ArrayList<>();
    private static final String SOURCE_CODE_URL = "https://github.com/lyyako/altera-patches";

    private SettingsRegistry() {
    }

    public static List<SettingsEntry> getSettings() {
        SETTINGS.clear();
        registerBaseSettings();
        registerSettings();
        Collections.sort(SETTINGS, (first, second) ->
            Integer.compare(first.order, second.order));
        return SETTINGS;
    }

    public static void add(SettingsEntry setting) {
        SETTINGS.add(setting);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        Context context = getContext();
        return context == null ? defaultValue :
            PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
    }

    private static Context getContext() {
        try {
            return ((Context) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null))
                .getApplicationContext();
        } catch (ReflectiveOperationException | NullPointerException ignored) {
            return null;
        }
    }

    private static void registerBaseSettings() {
        add(
            SettingsEntry.linkEntry(
                "altera_settings_information_title",
                "altera_settings_source_code_title",
                "altera_settings_source_code_summary",
                SOURCE_CODE_URL,
                0
            )
        );
    }

    private static void registerSettings() {
    }
}
