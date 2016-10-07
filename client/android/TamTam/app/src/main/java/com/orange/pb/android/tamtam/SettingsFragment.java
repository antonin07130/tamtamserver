package com.orange.pb.android.tamtam;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.android.volley.RequestQueue;

/**
 * Created by pascalbodin on 05/10/2016.
 */

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static String LOG_TAG = "SettingsFragment";

    // Must be set to same string than in xml/preferences.xml.
    private final static String PREF_PORT = "pref_port";

    /**
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Load the preferences.
        addPreferencesFromResource(R.xml.preferences);

    }

    /**
     *
     */
    @Override
    public void onResume() {

        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    /**
     *
     */
    @Override
    public void onPause() {

        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();

    }

    /**
     *
     * @param sharedPreferences
     * @param s
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        if (s.equals(PREF_PORT)) {
            String port = sharedPreferences.getString(PREF_PORT, MainActivity.DEF_PORT);
            AppLog.d(LOG_TAG, "Port changed. New value: " + port);
            // Reset Volley request queue.
            RequestQueue requestQueue = AppRequestQueue.getInstance(this.getActivity()
                    .getApplicationContext()).getRequestQueue();
            requestQueue.stop();
            requestQueue.start();
            return;
        }
        AppLog.d(LOG_TAG, "Unknown preference changed");

    }
}
