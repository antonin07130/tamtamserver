package com.orange.pb.android.tamtam;

/**
 * Created by pascalb on 8/18/16.
 */
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by pascalbodin on 15/08/2016.
 */
public class AppLog {

    private final static int LOG_MAX_NB = 128;
    private final static String DASH = " - ";
    private static ArrayList<String> mLogMessages = new ArrayList<String>();
    private static LogFragment mFragment = null;
    private final static SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static void setFragment(LogFragment fragment) {

        mFragment = fragment;

    }

    /**
     * Called by log fragment, when it's displayed again, to refresh display.
     */
    public static ArrayList<String> getLogMsg() {

        return mLogMessages;
    }

    public static void d(String tag, String msg) {

        // Log to standard Android log system.
        Log.d(tag, msg);
        // Display a log message in our log view.
        displayOurLogMsg(tag, msg);
    }

    private static void displayOurLogMsg(String tag, String msg) {

        StringBuilder logMsg = new StringBuilder();
        String formattedDate = LOG_TIME_FORMAT.format(Calendar.getInstance().getTime());
        logMsg.append(formattedDate).append(DASH).append(tag).append(DASH).append(msg);
        mLogMessages.add(0, logMsg.toString());
        // We may be requested to display a log while associated fragment is not active.
        if (mFragment == null) {
            return;
        }
        mFragment.displayLog(mLogMessages);

    }
}
