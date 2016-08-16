package com.example.android.slidingtabsbasic;

import android.util.Log;

/**
 * Created by pascalbodin on 15/08/2016.
 */
public class AppLog {

    private final static String DASH = " - ";
    private static SlidingTabsBasicFragment mFragment = null;

    public static void setFragment(SlidingTabsBasicFragment fragment) {

        mFragment = fragment;

    }

    public static void d(String tag, String msg) {

        // Log to standard Android log system.
        Log.d(tag, msg);
        // Display a log message in our log view.
        displayOurLogMsg(tag, msg);
    }

    private static void displayOurLogMsg(String tag, String msg) {

        if (mFragment == null) {
            return;
        }
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(tag).append(DASH).append(msg);
        mFragment.addLog(logMsg.toString());

    }
}
