package com.orange.pb.android.tamtam;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by pascalbodin on 05/10/2016.
 */

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Load the preferences.
        addPreferencesFromResource(R.xml.preferences);

    }

}
