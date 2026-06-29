package app.altera.extension.tiktok.sanitize;

import app.altera.extension.tiktok.settings.SettingsEntry;
import app.altera.extension.tiktok.settings.SettingsRegistry;

@SuppressWarnings("unused")
public final class ShareUrlSanitizer {
    public static final String SETTING_KEY = "altera_sanitize_sharing_links";

    private ShareUrlSanitizer() {
    }

    public static String stripAllQueryParams(String url) {
        int queryIndex = url == null || !SettingsRegistry.getBoolean(SETTING_KEY, true)
            ? -1
            : url.indexOf('?');
        return queryIndex > 0 ? url.substring(0, queryIndex) : url;
    }

    public static void registerSettings() {
        SettingsRegistry.add(
            SettingsEntry.switchEntry(
                "altera_settings_miscellaneous_title",
                "altera_settings_sanitize_sharing_links_title",
                "altera_settings_sanitize_sharing_links_summary",
                SETTING_KEY,
                true,
                100
            )
        );
    }
}
