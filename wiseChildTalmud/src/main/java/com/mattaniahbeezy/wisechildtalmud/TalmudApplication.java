package com.mattaniahbeezy.wisechildtalmud;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

/**
 * Created by Mattaniah on 1/17/2016.
 */
public class TalmudApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "HFqId1lr6Y3iQOJAB74KYX9J4iJ7PddcGJrXSPjC", "qsMlh9B8ee4cbQfiPQ0CqQB5q9N98dzvgf6XSdkj");
        ParseInstallation.getCurrentInstallation().saveInBackground();

        ParseUser.enableAutomaticUser();
        ParseUser user = ParseUser.getCurrentUser();
        user.increment("RunCount");
        user.saveInBackground();

    }
}
