package com.orange.pb.android.tamtam;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by pascalbodin on 05/10/2016.
 */

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Display settings fragment.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

}
