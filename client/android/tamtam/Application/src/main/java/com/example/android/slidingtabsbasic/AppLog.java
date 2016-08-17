package com.example.android.slidingtabsbasic;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by pascalbodin on 15/08/2016.
 */
public class AppLog {

    private final static String DASH = " - ";
    private static SlidingTabsBasicFragment mFragment = null;
    private final static SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

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
        String formattedDate = LOG_TIME_FORMAT.format(Calendar.getInstance().getTime());
        logMsg.append(formattedDate).append(DASH).append(tag).append(DASH).append(msg);
        mFragment.addLog(logMsg.toString());

    }
}
