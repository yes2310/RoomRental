package com.example.bangbillija;

import android.app.Application;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class BangBillijaApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
    }
}
