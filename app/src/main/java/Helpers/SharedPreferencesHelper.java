package Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;

import org.codewiz.droid2600.BuildConfig;

import java.util.List;


public final class SharedPreferencesHelper {

    private static final String TAG = SharedPreferencesHelper.class.getName();

    public enum Property {
        IS_SUBSCRIBED("is_subscribed");

        private String value;

        Property(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private static SharedPreferences getPreferences(final Context context) {

        return context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    public static String get(@NonNull final Context context,
                             @NonNull final Property property,
                             @NonNull final String defaultValue) {

        SharedPreferences sharedPreferences = getPreferences(context);
        String str = sharedPreferences.getString(property.value(), defaultValue);

        return str;
    }

    public static void put(@NonNull final Context context,
                           @NonNull final Property property,
                           @NonNull final String value) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(property.value(), value);
        editor.apply();
    }

    public static int get(@NonNull final Context context,
                          @NonNull final Property property,
                          final int defaultValue) {
        return getPreferences(context).getInt(property.value(), defaultValue);
    }

    public static void put(@NonNull final Context context,
                           @NonNull final Property property,
                           final int value) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt(property.value(), value);
        editor.apply();
    }

    public static boolean get(@NonNull final Context context,
                              @NonNull final Property property,
                              final boolean defaultValue) {
        return getPreferences(context).getBoolean(property.value(), defaultValue);
    }

    public static void put(@NonNull final Context context,
                           @NonNull final Property property,
                           final boolean value) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putBoolean(property.value(), value);
        editor.apply();
    }

    public static boolean has(@NonNull final Context context, @NonNull final Property property) {
        return get(context, property, "").length() > 0;
    }

    public static void remove(@NonNull final Context context,
                              @NonNull final Property... properties) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        try {
            for (Property property : properties) {
                editor.remove(property.value());
            }
            editor.apply();
        } catch (Exception ex) {
            Log.e(TAG, "FUNCTION : remove ", ex);
        }
    }

    public static <T> void put(@NonNull final Context context,
                               @NonNull final Property property,
                               final List<T> list) {

        Gson gson = new Gson();
        String json = gson.toJson(list);

        SharedPreferences.Editor editor = getPreferences(context).edit();

        editor.putString(property.value(), json);
        editor.apply();
    }
}