package com.kadabra.courier.utilities;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;



public class AppController extends Application {
    private static AppController mContext;

    @Override
    public void onCreate() {
        super.onCreate();
//        Fabric.with(this, new Crashlytics());
        mContext = this;

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        LocaleManager.INSTANCE.setLocale(this);
    }

    public static synchronized AppController getInstance() {
        return mContext;
    }

    public static Context getContext() {
        return mContext;
    }


}
