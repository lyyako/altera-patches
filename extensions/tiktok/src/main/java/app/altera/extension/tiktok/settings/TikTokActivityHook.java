package app.altera.extension.tiktok.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"deprecation", "unused"})
public final class TikTokActivityHook {
    private static final String SETTINGS_EXTRA = "altera_settings";
    private static final String SETTINGS_ACTIVITY =
        "com.bytedance.ies.ugc.aweme.commercialize.compliance.personalization.AdPersonalizationActivity";
    private static final String SETTINGS_TITLE = "Altera";
    private static final String STRING_CATEGORY_TITLE = "altera_settings_category_title";

    private static int pageLayoutId;
    private static int categoryLayoutId;
    private static int switchLayoutId;
    private static int rightIconLayoutId;
    private static int pageBackgroundAttributeId;
    private static int cardBackgroundAttributeId;
    private static Object settingsCategory;
    private static String settingsCategoryTitle;

    private TikTokActivityHook() {
    }

    public static void setResourceIds(
        int pageLayout,
        int categoryLayout,
        int switchLayout,
        int rightIconLayout,
        int pageBackgroundAttribute,
        int cardBackgroundAttribute
    ) {
        pageLayoutId = pageLayout;
        categoryLayoutId = categoryLayout;
        switchLayoutId = switchLayout;
        rightIconLayoutId = rightIconLayout;
        pageBackgroundAttributeId = pageBackgroundAttribute;
        cardBackgroundAttributeId = cardBackgroundAttribute;
    }

    public static boolean initialize(Activity activity) {
        if (!isSettingsActivity(activity)) {
            return false;
        }

        ViewGroup page = (ViewGroup) activity.getLayoutInflater().inflate(pageLayoutId, null);
        ViewGroup.LayoutParams contentLayoutParams = page.getChildAt(1).getLayoutParams();
        page.removeViewAt(1);

        int pageColor = resolveColor(activity, pageBackgroundAttributeId);
        page.setBackgroundColor(pageColor);
        configureNavigation(activity, page.getChildAt(0), pageColor);
        page.addView(createContent(activity, pageColor), 1, contentLayoutParams);

        activity.setContentView(page);
        configureSystemBars(activity, pageColor);
        return true;
    }

    public static boolean isSettingsActivity(Activity activity) {
        return activity.getIntent().getBooleanExtra(SETTINGS_EXTRA, false);
    }

    public static void startSettingsActivity(Context context) {
        Intent intent = new Intent()
            .setClassName(context, SETTINGS_ACTIVITY)
            .putExtra(SETTINGS_EXTRA, true);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void setSettingsCategory(Object category, Context context) {
        settingsCategory = category;
        settingsCategoryTitle = getString(context, STRING_CATEGORY_TITLE);
    }

    public static String getSettingsCategoryTitle(Object category) {
        return category == settingsCategory ? settingsCategoryTitle : null;
    }

    public static String getSettingsTitle() {
        return SETTINGS_TITLE;
    }

    public static String getString(Context context, String name) {
        return context.getString(
            context.getResources().getIdentifier(name, "string", context.getPackageName())
        );
    }

    private static View createContent(Activity activity, int pageColor) {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        scrollView.setBackgroundColor(pageColor);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(pageColor);
        scrollView.addView(
            content,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        );

        List<SettingsEntry> settings = SettingsRegistry.getSettings();
        for (int index = 0; index < settings.size();) {
            String categoryResourceName = settings.get(index).categoryResourceName;
            content.addView(
                createCategory(activity, categoryResourceName, index == 0),
                horizontalMargins(activity)
            );

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable background = new GradientDrawable();
            background.setColor(resolveColor(activity, cardBackgroundAttributeId));
            background.setCornerRadius(dp(activity, 6));
            card.setBackground(background);
            card.setClipToOutline(true);

            while (
                index < settings.size() &&
                    settings.get(index).categoryResourceName.equals(categoryResourceName)
            ) {
                card.addView(
                    createRow(activity, settings.get(index)),
                    new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                );
                index++;
            }

            content.addView(card, horizontalMargins(activity));
        }

        return scrollView;
    }

    private static LinearLayout.LayoutParams horizontalMargins(Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMarginStart(dp(context, 8));
        params.setMarginEnd(dp(context, 8));
        return params;
    }

    private static View createCategory(Activity activity, String resourceName, boolean firstCategory) {
        View category = activity.getLayoutInflater().inflate(categoryLayoutId, null, false);
        if (!firstCategory) {
            hideCategoryTopSpacer(category);
        }
        List<TextView> textViews = findTextViews(category);
        textViews.get(0).setText(getString(activity, resourceName));
        category.setBackgroundColor(resolveColor(activity, pageBackgroundAttributeId));
        return category;
    }

    private static void hideCategoryTopSpacer(View category) {
        if (category instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) category;
            if (group.getChildCount() > 1) {
                group.getChildAt(1).setVisibility(View.GONE);
            }
        }
    }

    private static View createRow(Activity activity, SettingsEntry setting) {
        return setting.url == null
            ? createSwitchRow(activity, setting)
            : createLinkRow(activity, setting);
    }

    private static View createLinkRow(Activity activity, SettingsEntry setting) {
        View row = activity.getLayoutInflater().inflate(rightIconLayoutId, null, false);
        row.setBackgroundColor(Color.TRANSPARENT);
        bindLabels(activity, row, setting);
        bindClickListenerRecursively(
            row,
            view -> activity.startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(setting.url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        );

        return row;
    }

    private static void bindClickListenerRecursively(View view, View.OnClickListener listener) {
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnClickListener(listener);

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                bindClickListenerRecursively(group.getChildAt(index), listener);
            }
        }
    }

    private static View createSwitchRow(Activity activity, SettingsEntry setting) {
        View row = activity.getLayoutInflater().inflate(switchLayoutId, null, false);
        row.setBackgroundColor(Color.TRANSPARENT);
        TextView title = bindLabels(activity, row, setting);

        CompoundButton toggle = findCompoundButton(row);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        toggle.setChecked(preferences.getBoolean(setting.preferenceKey, setting.defaultValue));
        toggle.setOnCheckedChangeListener(
            (button, checked) -> preferences.edit()
                .putBoolean(setting.preferenceKey, checked)
                .apply()
        );
        toggle.setClickable(false);
        toggle.setFocusable(false);

        View container = findCommonAncestor(row, title, toggle);
        container.setClickable(true);
        container.setFocusable(true);
        container.setContentDescription(row.getContentDescription());
        container.setOnClickListener(view -> toggle.setChecked(!toggle.isChecked()));

        return row;
    }

    private static TextView bindLabels(
        Activity activity,
        View row,
        SettingsEntry setting
    ) {
        List<TextView> textViews = findTextViews(row);
        TextView title = textViews.get(0);
        TextView summary = textViews.get(1);
        title.setText(getString(activity, setting.titleResourceName));
        summary.setText(getString(activity, setting.summaryResourceName));
        summary.setVisibility(View.VISIBLE);
        summary.setPadding(
            summary.getPaddingLeft(),
            summary.getPaddingTop() + dp(activity, 6),
            summary.getPaddingRight(),
            summary.getPaddingBottom()
        );
        row.setContentDescription(title.getText() + ", " + summary.getText());
        return title;
    }

    private static void configureNavigation(Activity activity, View navigation, int pageColor) {
        preserveNavigationRegisters(activity, navigation, pageColor, new Object());
    }

    private static synchronized void preserveNavigationRegisters(
        Activity activity,
        View navigation,
        int pageColor,
        Object state
    ) {
    }

    private static List<TextView> findTextViews(View root) {
        List<TextView> result = new ArrayList<>();
        collectTextViews(root, result);
        return result;
    }

    private static void collectTextViews(View view, List<TextView> result) {
        if (view instanceof TextView) {
            result.add((TextView) view);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                collectTextViews(group.getChildAt(index), result);
            }
        }
    }

    private static CompoundButton findCompoundButton(View view) {
        if (view instanceof CompoundButton) {
            return (CompoundButton) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                CompoundButton result = findCompoundButton(group.getChildAt(index));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static View findCommonAncestor(View root, View first, View second) {
        ViewParent candidate = second.getParent();
        while (candidate instanceof View && candidate != root) {
            if (isAncestor((View) candidate, first)) {
                return (View) candidate;
            }
            candidate = candidate.getParent();
        }
        return root;
    }

    private static boolean isAncestor(View ancestor, View child) {
        ViewParent parent = child.getParent();
        while (parent instanceof View) {
            if (parent == ancestor) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static int resolveColor(Context context, int attributeId) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, value, true);
        return value.resourceId == 0 ? value.data : context.getColor(value.resourceId);
    }

    private static void configureSystemBars(Activity activity, int color) {
        Window window = activity.getWindow();
        window.setStatusBarColor(color);
        window.setNavigationBarColor(color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int visibility = window.getDecorView().getSystemUiVisibility();
            boolean light = isLight(color);
            visibility = setFlag(visibility, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR, light);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                visibility =
                    setFlag(visibility, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR, light);
            }
            window.getDecorView().setSystemUiVisibility(visibility);
        }
    }

    private static int setFlag(int value, int flag, boolean enabled) {
        return enabled ? value | flag : value & ~flag;
    }

    private static boolean isLight(int color) {
        return Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114
            >= 128000;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
