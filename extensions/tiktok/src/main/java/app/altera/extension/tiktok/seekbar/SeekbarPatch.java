package app.altera.extension.tiktok.seekbar;

import app.altera.extension.tiktok.settings.SettingsEntry;
import app.altera.extension.tiktok.settings.SettingsRegistry;

@SuppressWarnings("unused")
public final class SeekbarPatch {
    public static final String SETTING_KEY = "altera_show_seekbar";

    private SeekbarPatch() {
    }

    public static boolean isEnabled() {
        return SettingsRegistry.getBoolean(SETTING_KEY, true);
    }

    public static int overrideSeekbarShowType(int seekbarType) {
        return isEnabled() && (seekbarType == 3 || seekbarType == 4) ? 0 : seekbarType;
    }

    public static void registerSettings() {
        SettingsRegistry.add(
            SettingsEntry.switchEntry(
                "altera_settings_miscellaneous_title",
                "altera_settings_show_seekbar_title",
                "altera_settings_show_seekbar_summary",
                SETTING_KEY,
                true,
                110
            )
        );
    }
}
