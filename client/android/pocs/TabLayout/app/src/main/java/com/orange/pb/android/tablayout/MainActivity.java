package com.orange.pb.android.tablayout;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

/**
 * Created by pascalb on 8/17/16.
 */
public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    private final static String LOG_TAG = "MainActivity";

    private final static boolean AUTO_REFRESH = true;

    // This is our tablayout
    private TabLayout tabLayout;

    // This is our viewPager
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        // Adding toolbar to the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        // Adding the tabs using addTab() method.
        // Ta titles can't be set here, as we call tabLayout.setupWithViewPager() below.
        tabLayout.addTab(tabLayout.newTab());  // My things
        tabLayout.addTab(tabLayout.newTab());  // Around me
        tabLayout.addTab(tabLayout.newTab());  // Tracked things
        tabLayout.addTab(tabLayout.newTab());  // Log
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // Initializing viewPager.
        viewPager = (ViewPager) findViewById(R.id.pager);
        // Creating our pager adapter. Tab titles must be set here.
        String[] tabTitles = new String[4];
        tabTitles[0] = getResources().getString(R.string.tab1_title);
        tabTitles[1] = getResources().getString(R.string.tab2_title);
        tabTitles[2] = getResources().getString(R.string.tab3_title);
        tabTitles[3] = getResources().getString(R.string.tab_log);
        Pager adapter = new Pager(getSupportFragmentManager(), tabTitles);
        // Adding adapter to pager.
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager, AUTO_REFRESH);
        // Adding onTabSelectedListener to swipe views
        tabLayout.addOnTabSelectedListener(this);

    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        viewPager.setCurrentItem(tab.getPosition());
        AppLog.d(LOG_TAG, "onTabSelected()");

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

}
