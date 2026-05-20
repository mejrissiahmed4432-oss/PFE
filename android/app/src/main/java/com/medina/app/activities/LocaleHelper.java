package com.medina.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LocaleHelper {
    public static Context onAttach(Context context) {
        String lang = getPersistedLanguage(context, Locale.getDefault().getLanguage());
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        return getPersistedLanguage(context, Locale.getDefault().getLanguage());
    }

    public static Context setLocale(Context context, String language) {
        persistLanguage(context, language);
        return updateResources(context, language);
    }

    private static String getPersistedLanguage(Context context, String defaultLanguage) {
        SharedPreferences preferences = context.getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        return preferences.getString("app_lang", defaultLanguage);
    }

    private static void persistLanguage(Context context, String language) {
        SharedPreferences preferences = context.getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        preferences.edit().putString("app_lang", language).apply();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        
        // Update both context and legacy resources to ensure backward compatibility
        context = context.createConfigurationContext(configuration);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }
}
