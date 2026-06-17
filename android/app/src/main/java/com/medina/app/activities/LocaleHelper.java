package com.medina.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_KEY = "app_lang";

    /** Call this in every Activity's attachBaseContext() */
    public static Context onAttach(Context context) {
        String lang = getSavedLanguage(context);
        return applyLocale(context, lang);
    }

    /** Returns the persisted language code (defaults to device language) */
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        return prefs.getString(PREF_KEY, Locale.getDefault().getLanguage());
    }

    /** Saves the language and applies it; returns the updated context */
    public static Context setLocale(Context context, String language) {
        saveLanguage(context, language);
        return applyLocale(context, language);
    }

    private static void saveLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences("medina_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_KEY, language).apply();
    }

    private static Context applyLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        // createConfigurationContext is the modern, non-deprecated way
        return context.createConfigurationContext(config);
    }
}
