package com.kadabra.courier.utilities;

import android.app.Application;
import android.content.Context;

import com.reach.plus.admin.util.UserSessionManager;
import com.kadabra.courier.R;

import java.util.Locale;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class AppController extends Application {
    private static AppController mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        if (UserSessionManager.Companion.getInstance(this).getLanguage() == AppConstants.INSTANCE.getENGLISH())
            LanguageUtil.changeLanguageType(mContext, new Locale(AppConstants.INSTANCE.getENGLISH()));
        else
            LanguageUtil.changeLanguageType(mContext, new Locale(AppConstants.INSTANCE.getARABIC()));

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Montserrat-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
    }


    public static synchronized AppController getInstance() {
        return mContext;
    }

    public static Context getContext() {
        return mContext;
    }


}
