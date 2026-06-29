package app.altera.extension.tiktok.settings;

public final class SettingsEntry {
    final String categoryResourceName;
    final String titleResourceName;
    final String summaryResourceName;
    final String preferenceKey;
    final boolean defaultValue;
    final String url;
    final int order;

    private SettingsEntry(
        String categoryResourceName,
        String titleResourceName,
        String summaryResourceName,
        String preferenceKey,
        boolean defaultValue,
        String url,
        int order
    ) {
        this.categoryResourceName = categoryResourceName;
        this.titleResourceName = titleResourceName;
        this.summaryResourceName = summaryResourceName;
        this.preferenceKey = preferenceKey;
        this.defaultValue = defaultValue;
        this.url = url;
        this.order = order;
    }

    public static SettingsEntry switchEntry(
        String categoryResourceName,
        String titleResourceName,
        String summaryResourceName,
        String preferenceKey,
        boolean defaultValue,
        int order
    ) {
        return new SettingsEntry(
            categoryResourceName,
            titleResourceName,
            summaryResourceName,
            preferenceKey,
            defaultValue,
            null,
            order
        );
    }

    public static SettingsEntry linkEntry(
        String categoryResourceName,
        String titleResourceName,
        String summaryResourceName,
        String url,
        int order
    ) {
        return new SettingsEntry(
            categoryResourceName,
            titleResourceName,
            summaryResourceName,
            null,
            false,
            url,
            order
        );
    }
}
