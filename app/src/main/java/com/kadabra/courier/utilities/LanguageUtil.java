package com.kadabra.courier.utilities;


import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;
import java.util.Locale;

public class LanguageUtil {
    public static void changeLanguageType(Context context, Locale newLocale) {

        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (VersionUtils.isAfter24()) {
                configuration.setLocale(newLocale);

                LocaleList localeList = new LocaleList(newLocale);
                LocaleList.setDefault(localeList);
                configuration.setLocales(localeList);

            } else if (VersionUtils.isAfter17()) {
                configuration.setLocale(newLocale);

            } else {
                configuration.locale = newLocale;
                res.updateConfiguration(configuration, res.getDisplayMetrics());
            }
        }
        Locale.setDefault(newLocale);
        configuration.locale = LanguageUtil.getLanguageType(AppController.getContext()); // or whichever locale you desire
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(LanguageUtil.getLanguageType(AppController.getContext()));
        }
        Resources.getSystem().updateConfiguration(configuration, res.getDisplayMetrics());

    }

    public static Locale getLanguageType(Context context) {
        Log.i("=======", "context = " + context);
//        Resources resources = context.getResources();
        Resources resources = AppController.getContext().getResources();
        Configuration config = resources.getConfiguration();
        // Application user selection language
        if (VersionUtils.isAfter24()) {
            return config.getLocales().get(0);
        } else {
            return config.locale;
        }
    }
}
